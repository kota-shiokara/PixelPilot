package jp.ikanoshiokara.pixelpilot.android.hostselect

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice

/** 新しいホストとのペアリング(検出可能にする)の進み具合。 */
sealed interface PairingState {
    /** 検出可能にしていない。 */
    data object Idle : PairingState

    /** 検出可能にしている。残り [remainingSeconds] 秒。 */
    data class Discoverable(val remainingSeconds: Int, val totalSeconds: Int) : PairingState

    /** 検出可能にする時間が終わった(PC から見つけられなかった)。 */
    data object Expired : PairingState
}

/**
 * ホスト選択画面に出すもの。画面デザインの仮案 1・1-a〜1-g、設計書 7.5 のとおり。
 * v2.0 は「Bluetooth で接続」のカードのみ(USB のカードは USB 経路の実装後に足す)。
 */
sealed interface HostSelectUiState {
    /** HID 対応の確認と登録の途中(1-e)。 */
    data object Checking : HostSelectUiState

    /**
     * 権限が必要(1-a)。
     *
     * @property blocked 「今後表示しない」になっている(1-b)。アプリの設定から許可してもらう。
     */
    data class NeedsPermission(val blocked: Boolean) : HostSelectUiState

    /** Bluetooth がオフ(1-c)。 */
    data object BluetoothOff : HostSelectUiState

    /** この端末は Bluetooth HID に対応していない(1-d)。 */
    data object Unsupported : HostSelectUiState

    /**
     * 接続先を選ぶ(1、1-f、1-g)。
     *
     * @property lastHost 前回接続したホスト(ペアリング済みの場合だけ)。
     * @property otherHosts 前回のホスト以外のペアリング済みの PC。
     * @property connecting 接続しようとしているホスト。
     * @property error 直前の接続のエラー。
     */
    data class Hosts(
        val lastHost: HostDevice?,
        val otherHosts: List<HostDevice>,
        val connecting: HostDevice?,
        val error: ErrorReason?,
        val pairing: PairingState,
    ) : HostSelectUiState

    /** 接続できた。メイン画面(#45)へ進む。 */
    data class Connected(val host: HostDevice) : HostSelectUiState
}

/**
 * 接続状態・権限・保存してある前回のホスト・ペアリング済みの一覧・ペアリングの進み具合から、画面に出すものを決める。
 * Android に依存しない純 Kotlin の関数にして、単体テストする。
 */
fun hostSelectUiState(
    connection: ConnectionState,
    permissionBlocked: Boolean,
    lastHostAddress: String?,
    pairedHosts: List<HostDevice>,
    pairing: PairingState,
): HostSelectUiState {
    fun hosts(connecting: HostDevice?, error: ErrorReason?): HostSelectUiState.Hosts {
        val lastHost = pairedHosts.firstOrNull { it.address == lastHostAddress }
        return HostSelectUiState.Hosts(
            lastHost = lastHost,
            otherHosts = pairedHosts.filter { it.address != lastHost?.address },
            connecting = connecting,
            error = error,
            pairing = pairing,
        )
    }

    return when (connection) {
        ConnectionState.Registering -> HostSelectUiState.Checking
        ConnectionState.PermissionRequired -> HostSelectUiState.NeedsPermission(blocked = permissionBlocked)
        ConnectionState.BluetoothOff -> HostSelectUiState.BluetoothOff
        ConnectionState.Unsupported -> HostSelectUiState.Unsupported
        ConnectionState.Ready -> hosts(connecting = null, error = null)
        is ConnectionState.Connecting -> hosts(connecting = connection.host, error = null)
        is ConnectionState.Connected -> HostSelectUiState.Connected(connection.host)
        is ConnectionState.Error -> when (connection.reason) {
            // ホスト選択画面では、オフはエラーではなく案内として出す(7.5)
            ErrorReason.BLUETOOTH_OFF -> HostSelectUiState.BluetoothOff
            else -> hosts(connecting = null, error = connection.reason)
        }
    }
}
