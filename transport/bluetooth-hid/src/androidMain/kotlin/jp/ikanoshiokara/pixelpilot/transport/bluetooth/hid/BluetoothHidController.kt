package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import jp.ikanoshiokara.pixelpilot.domain.hid.HidReport
import jp.ikanoshiokara.pixelpilot.domain.hid.HidReportDescriptor
import jp.ikanoshiokara.pixelpilot.domain.hid.ReportId
import jp.ikanoshiokara.pixelpilot.domain.hid.ReportLength
import jp.ikanoshiokara.pixelpilot.domain.settings.HostRepository
import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardLeds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Bluetooth HID Device プロファイルの登録とホストへの接続を管理し、[ConnectionState] を公開する。
 *
 * 流れ(docs/design/v2.0-standard.md 7.2):
 * 1. 権限の確認 → Bluetooth のオン / オフ → `getProfileProxy(HID_DEVICE)`(最大 [PROFILE_TIMEOUT_MILLIS] 待つ)
 *    → `registerApp` → `onAppStatusChanged(registered = true)` で [ConnectionState.Ready]。
 * 2. 登録できたら、前回のホスト([HostRepository])に自動で接続する。ペアリング済みの一覧([pairedHosts])から
 *    選んで [connect] することもできる。新しいホストは、検出可能にして([Discoverable])PC 側から追加してもらう。
 * 3. 接続できたホストを保存する。使用中に切れたら、[RECONNECT_ATTEMPTS] 回まで再接続を試みる。
 *    ユーザーの操作で切断した場合([disconnect])は再接続しない。
 *
 * - 権限の要求そのもの(ダイアログ)はアプリ側で行い、許可されたら [start] を呼ぶ。
 * - アプリがフォアグラウンドに来たら [start]、バックグラウンドに回ったら [stop] を呼ぶ(#44・#45 でつなぐ)。
 * - レポートの送信は [sendReport](送信キュー BluetoothHidTransport から呼ぶ)。
 * - 状態の遷移は [reduce](純 Kotlin、単体テストあり)で決める。このクラスは Android の API の結果を
 *   [HidConnectionSignal] に置き換えて渡すだけにする。
 *
 * Android の Bluetooth API を呼ぶ箇所は、権限の確認([hasConnectPermission])を通った後でしか呼ばないため、
 * クラス全体で MissingPermission の警告を抑止している。
 */
@SuppressLint("MissingPermission")
class BluetoothHidController(
    private val context: Context,
    private val platformTransports: AndroidPlatformTransports,
    private val hostRepository: HostRepository,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Registering)

    /** Bluetooth HID 経路の接続状態。 */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _keyboardLeds = MutableStateFlow(KeyboardLeds())

    /** ホストから通知されたキーボードの LED。接続が切れたら初期値に戻す。 */
    val keyboardLeds: StateFlow<KeyboardLeds> = _keyboardLeds.asStateFlow()

    /**
     * HID のコールバックを受けるスレッド。送信キュー(BluetoothHidTransport)も同じスレッドで直列化し、
     * 接続・切断の通知とレポートの送信が前後しないようにする。
     */
    internal val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val lock = Any()
    private var started = false
    private var receiverRegistered = false
    private var profileTimeout: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    /** 接続中・接続しようとしているホスト。再接続に使う。 */
    @Volatile
    private var currentHost: HostDevice? = null

    /** [disconnect] で切断を要求した。切断の通知が来ても再接続しない。 */
    @Volatile
    private var disconnectRequested = false

    /** 接続中のホスト(レポートの送信先)。未接続なら null。 */
    @Volatile
    private var connectedDevice: BluetoothDevice? = null

    /** 登録済みのプロファイルプロキシ。未取得・解放済みなら null。 */
    @Volatile
    internal var hidDevice: BluetoothHidDevice? = null
        private set

    /** HID アプリの登録を始める。権限の許可後・フォアグラウンド復帰時に呼ぶ。開始済みなら何もしない。 */
    fun start() {
        synchronized(lock) {
            if (started) return
            if (!hasConnectPermission()) {
                signal(HidConnectionSignal.PermissionMissing)
                return
            }
            val adapter = adapter
            if (adapter == null) {
                markUnsupported()
                return
            }
            started = true
            registerBluetoothStateReceiver()
            if (adapter.isEnabled) {
                requestProfile(adapter)
            } else {
                signal(HidConnectionSignal.BluetoothTurnedOff)
            }
        }
    }

    /** 登録を解除し、プロファイルプロキシを閉じる。バックグラウンドに回ったとき・終了時に呼ぶ。 */
    fun stop() {
        synchronized(lock) {
            if (!started) return
            started = false
            cancelReconnect()
            unregisterBluetoothStateReceiver()
            releaseProfile()
            signal(HidConnectionSignal.Stopped)
        }
    }

    /**
     * ペアリング済みの機器のうち、PC(Bluetooth の機器クラスがコンピュータ)のもの。
     * イヤホンなど、マウス・キーボードの接続先にならない機器は含めない。
     */
    fun pairedHosts(): List<HostDevice> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices.orEmpty()
            .filter { it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER }
            .map { it.toHostDevice() }
    }

    /** ホストに接続する。登録済みでない(プロファイルを取得していない)ときは何もしない。 */
    fun connect(host: HostDevice) {
        cancelReconnect()
        connectInternal(host)
    }

    /** ホストとの接続を切る(接続先を選び直すとき)。再接続はしない。 */
    fun disconnect() {
        cancelReconnect()
        val device = hidDevice
        val host = currentHost
        val adapter = adapter
        if (device == null || host == null || adapter == null) {
            signal(HidConnectionSignal.DisconnectedByUser)
            return
        }
        disconnectRequested = true
        // 切断の完了は onConnectionStateChanged で受け取る
        if (!device.disconnect(adapter.getRemoteDevice(host.address))) {
            disconnectRequested = false
            signal(HidConnectionSignal.DisconnectedByUser)
        }
    }

    /** 接続中のホストへレポートを送る。未接続なら送らずに false を返す。送信キューのスレッドから呼ぶ。 */
    internal fun sendReport(report: HidReport): Boolean {
        val device = hidDevice ?: return false
        val host = connectedDevice ?: return false
        return device.sendReport(host, report.reportId, report.data)
    }

    private fun connectInternal(host: HostDevice) {
        val device = hidDevice ?: return
        val adapter = adapter ?: return
        disconnectRequested = false
        currentHost = host
        if (!device.connect(adapter.getRemoteDevice(host.address))) {
            // 接続を始められなかった。つながらなかった場合と同じ扱い(HOST_NOT_FOUND)にする
            signal(HidConnectionSignal.HostConnecting(host))
            signal(HidConnectionSignal.HostDisconnected(host))
        }
    }

    /** 登録できたときに、前回のホストへ自動で接続する。 */
    private fun connectToLastHost() {
        scope.launch {
            val address = hostRepository.lastHostAddress.first() ?: return@launch
            val adapter = adapter ?: return@launch
            if (state.value == ConnectionState.Ready) {
                connectInternal(adapter.getRemoteDevice(address).toHostDevice())
            }
        }
    }

    /** 使用中に切れたときに、少し待ってから再接続を試みる。[RECONNECT_ATTEMPTS] 回で諦める。 */
    private fun scheduleReconnect() {
        val host = currentHost ?: return
        if (reconnectAttempts >= RECONNECT_ATTEMPTS) return
        reconnectAttempts++
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_INTERVAL_MILLIS)
            if (state.value.shouldReconnect) connectInternal(host)
        }
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
    }

    private fun requestProfile(adapter: BluetoothAdapter) {
        signal(HidConnectionSignal.RegisterStarted)
        val requested = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        if (!requested) {
            markUnsupported()
            return
        }
        profileTimeout?.cancel()
        profileTimeout = scope.launch {
            delay(PROFILE_TIMEOUT_MILLIS)
            if (hidDevice == null) markUnsupported()
        }
    }

    private fun releaseProfile() {
        profileTimeout?.cancel()
        profileTimeout = null
        onHostDisconnected()
        val device = hidDevice ?: return
        hidDevice = null
        device.unregisterApp()
        adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
    }

    private fun markUnsupported() {
        signal(HidConnectionSignal.ProfileUnavailable)
        platformTransports.markUnavailable(TransportKind.BLUETOOTH_HID)
    }

    private fun signal(signal: HidConnectionSignal) {
        _state.update { reduce(it, signal) }
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            synchronized(lock) {
                profileTimeout?.cancel()
                val device = proxy as BluetoothHidDevice
                if (!started) {
                    // 取得を待つ間に停止された
                    adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
                    return
                }
                hidDevice = device
                val registered = device.registerApp(sdpSettings(), null, qosSettings(), executor, hidCallback)
                if (!registered) signal(HidConnectionSignal.RegisterFailed)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            signal(HidConnectionSignal.RegisterFailed)
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                signal(HidConnectionSignal.Registered)
                connectToLastHost()
            } else {
                signal(HidConnectionSignal.RegisterFailed)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val host = device.toHostDevice()
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> {
                    currentHost = host
                    signal(HidConnectionSignal.HostConnecting(host))
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    currentHost = host
                    connectedDevice = device
                    disconnectRequested = false
                    cancelReconnect()
                    signal(HidConnectionSignal.HostConnected(host))
                    scope.launch { hostRepository.saveLastHost(host.address) }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    onHostDisconnected()
                    if (disconnectRequested) {
                        disconnectRequested = false
                        signal(HidConnectionSignal.DisconnectedByUser)
                    } else {
                        signal(HidConnectionSignal.HostDisconnected(host))
                        if (this@BluetoothHidController.state.value.shouldReconnect) scheduleReconnect()
                    }
                }
            }
        }

        /** ホストが LED の状態を送ってきた(SET_REPORT)。応答を返さないとホストが待たされる。 */
        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            val accepted = type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id.toInt() == ReportId.KEYBOARD
            if (accepted) updateLeds(data)
            hidDevice?.reportError(
                device,
                if (accepted) BluetoothHidDevice.ERROR_RSP_SUCCESS else BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID,
            )
        }

        /** ホストが LED の状態を送ってきた(割り込みチャネル)。 */
        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if (reportId.toInt() == ReportId.KEYBOARD) updateLeds(data)
        }

        /**
         * ホストが現在のレポートを求めてきた(GET_REPORT)。押下なし・移動なしのレポートを返す。
         * 応答しないとホストによっては接続を切るため、必ず応答する。
         */
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val length = when (id.toInt()) {
                ReportId.KEYBOARD -> ReportLength.KEYBOARD_INPUT
                ReportId.MOUSE -> ReportLength.MOUSE_INPUT
                ReportId.CONSUMER -> ReportLength.CONSUMER_INPUT
                else -> null
            }
            val hid = hidDevice ?: return
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && length != null) {
                hid.replyReport(device, type, id, ByteArray(length))
            } else {
                hid.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }
        }
    }

    private fun updateLeds(data: ByteArray) {
        if (data.isNotEmpty()) _keyboardLeds.value = KeyboardLeds(data[0].toInt() and 0xFF)
    }

    private fun onHostDisconnected() {
        connectedDevice = null
        _keyboardLeds.value = KeyboardLeds()
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> synchronized(lock) {
                    // オフになるとプロキシは使えなくなる。オンに戻ったら取り直し、前回のホストへ接続する
                    cancelReconnect()
                    releaseProfile()
                    signal(HidConnectionSignal.BluetoothTurnedOff)
                }
                BluetoothAdapter.STATE_ON -> synchronized(lock) {
                    val adapter = adapter
                    if (started && adapter != null) requestProfile(adapter)
                }
            }
        }
    }

    private fun registerBluetoothStateReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!receiverRegistered) return
        context.unregisterReceiver(bluetoothStateReceiver)
        receiverRegistered = false
    }

    private fun BluetoothDevice.toHostDevice() = HostDevice(address = address, name = name)

    /**
     * SDP に載せる情報。名前はホスト側の Bluetooth 設定の表示に使われることがある。
     * Subclass はキーボード・マウスの複合(設計書 2.)。
     * クラスの読み込み時ではなく登録時に作る(JVM の単体テストで Android のクラスを初期化しないため)。
     */
    private fun sdpSettings() = BluetoothHidDeviceAppSdpSettings(
        "PixelPilot",
        "Mouse and keyboard",
        "PixelPilot",
        BluetoothHidDevice.SUBCLASS1_COMBO,
        HidReportDescriptor.bytes,
    )

    /**
     * 送信(Input)側の QoS。低遅延を要求する。効き方は端末依存のため、実機で計測して調整する(#48)。
     * 値の意味: サービス種別、トークンレート、バケットサイズ、ピーク帯域、遅延(マイクロ秒)、遅延のばらつき。
     */
    private fun qosSettings() = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800,
        9,
        0,
        11_250,
        BluetoothHidDeviceAppQosSettings.MAX,
    )

    private companion object {
        /** HID 非対応と判定するまでの待ち時間(docs/design/v2.0-standard.md 13. 決定事項 14)。 */
        const val PROFILE_TIMEOUT_MILLIS = 5_000L

        /** 使用中に切れたときに再接続を試みる回数と間隔。 */
        const val RECONNECT_ATTEMPTS = 3
        const val RECONNECT_INTERVAL_MILLIS = 2_000L
    }
}
