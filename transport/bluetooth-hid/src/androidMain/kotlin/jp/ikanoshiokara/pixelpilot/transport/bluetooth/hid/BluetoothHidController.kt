package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import jp.ikanoshiokara.pixelpilot.domain.hid.HidReportDescriptor
import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Bluetooth HID Device プロファイルの登録を管理し、[ConnectionState] を公開する。
 *
 * 流れ(docs/design/v2.0-standard.md 7.2 の 1〜4):
 * 権限の確認 → Bluetooth のオン / オフ → `getProfileProxy(HID_DEVICE)`(最大 [PROFILE_TIMEOUT_MILLIS] 待つ)
 * → `registerApp` → `onAppStatusChanged(registered = true)` で [ConnectionState.Ready]。
 *
 * - 権限の要求そのもの(ダイアログ)はアプリ側で行い、許可されたら [start] を呼ぶ。
 * - アプリがフォアグラウンドに来たら [start]、バックグラウンドに回ったら [stop] を呼ぶ(#44・#45 でつなぐ)。
 * - ホストへの接続と再接続は #37、レポートの送信は #38 で追加する。
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
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Registering)

    /** Bluetooth HID 経路の接続状態。 */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** HID のコールバックを受けるスレッド。送信キュー(#38)も同じスレッドで直列化する予定。 */
    internal val callbackExecutor: Executor = Executors.newSingleThreadExecutor()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val lock = Any()
    private var started = false
    private var receiverRegistered = false
    private var profileTimeout: Job? = null

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
            unregisterBluetoothStateReceiver()
            releaseProfile()
            signal(HidConnectionSignal.Stopped)
        }
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
                val registered = device.registerApp(sdpSettings(), null, qosSettings(), callbackExecutor, hidCallback)
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
            signal(if (registered) HidConnectionSignal.Registered else HidConnectionSignal.RegisterFailed)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val host = device.toHostDevice()
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> signal(HidConnectionSignal.HostConnecting(host))
                BluetoothProfile.STATE_CONNECTED -> signal(HidConnectionSignal.HostConnected(host))
                BluetoothProfile.STATE_DISCONNECTED -> signal(HidConnectionSignal.HostDisconnected(host))
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> synchronized(lock) {
                    // オフになるとプロキシは使えなくなる。オンに戻ったら取り直す
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
    }
}
