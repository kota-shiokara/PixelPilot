package jp.ikanoshiokara.pixelpilot.android.hostselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.ikanoshiokara.pixelpilot.domain.settings.HostRepository
import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ホスト選択画面の状態を持つ。画面はこの [uiState] を描画し、操作をこのクラスのメソッドに渡すだけにする。
 *
 * 権限のダイアログ・Bluetooth の有効化・検出可能化など、Activity が要る操作は画面側で行い、結果をここに渡す。
 */
class HostSelectViewModel(
    private val controller: BluetoothHidController,
    hostRepository: HostRepository,
) : ViewModel() {
    private val pairedHosts = MutableStateFlow<List<HostDevice>>(emptyList())
    private val permissionBlocked = MutableStateFlow(false)
    private val pairing = MutableStateFlow<PairingState>(PairingState.Idle)
    private var countdown: Job? = null

    val uiState: StateFlow<HostSelectUiState> =
        combine(
            controller.state,
            permissionBlocked,
            hostRepository.lastHostAddress,
            pairedHosts,
            pairing,
        ) { connection, blocked, lastHost, hosts, pairingState ->
            hostSelectUiState(connection, blocked, lastHost, hosts, pairingState)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HostSelectUiState.Checking)

    init {
        viewModelScope.launch {
            controller.state.collect { state ->
                when (state) {
                    // 登録できたら、ペアリング済みの一覧を読み直す(PC 側で追加された場合も含む)
                    ConnectionState.Ready, is ConnectionState.Error -> refreshPairedHosts()
                    // つながったらペアリングの案内を閉じる
                    is ConnectionState.Connected -> stopPairing()
                    else -> Unit
                }
            }
        }
    }

    /** ペアリング済みの一覧を読み直す。画面に戻ってきたときにも呼ぶ。 */
    fun refreshPairedHosts() {
        pairedHosts.value = controller.pairedHosts()
    }

    /** 権限が許可された。登録を始める。 */
    fun onPermissionGranted() {
        permissionBlocked.value = false
        controller.start()
    }

    /**
     * 権限が拒否された。
     *
     * @param blocked 「今後表示しない」になっている(もうダイアログを出せない)。
     */
    fun onPermissionDenied(blocked: Boolean) {
        permissionBlocked.value = blocked
    }

    fun connect(host: HostDevice) {
        controller.connect(host)
    }

    /** エラーの後の「もう一度」。登録に失敗した場合は登録からやり直し、それ以外は前回のホストへ接続し直す。 */
    fun retry() {
        val state = uiState.value as? HostSelectUiState.Hosts
        val host = state?.lastHost
        if (state != null && host != null && state.error != ErrorReason.REGISTER_FAILED) {
            controller.connect(host)
        } else {
            controller.stop()
            controller.start()
        }
    }

    /** 検出可能になった(システムのダイアログで許可された)。残り時間を数える。 */
    fun onDiscoverableStarted(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                pairing.value = PairingState.Discoverable(remainingSeconds = remaining, totalSeconds = seconds)
                delay(1_000)
            }
            pairing.value = PairingState.Expired
        }
    }

    /** ペアリングの案内を閉じる(検出可能な状態そのものは、時間が来るまでシステムが続ける)。 */
    fun stopPairing() {
        countdown?.cancel()
        countdown = null
        pairing.value = PairingState.Idle
    }
}
