package jp.ikanoshiokara.pixelpilot.model.connection

/**
 * 経路(v2.0 は Bluetooth HID)の接続状態。アプリ全体の状態ではない。UI はこの状態を描画するだけにする。
 *
 * docs/design/v2.0-standard.md 7.1 状態、7.5 権限と状態ごとの表示
 */
sealed interface ConnectionState {
    /** 権限(Android 12 以降は「付近のデバイス」)が必要。 */
    data object PermissionRequired : ConnectionState

    /** Bluetooth がオフ(接続中以外)。 */
    data object BluetoothOff : ConnectionState

    /** この端末では経路が使えない(HID Device プロファイルが無い・応答しない)。 */
    data object Unsupported : ConnectionState

    /** 準備中(HID 対応の確認と、HID アプリの登録)。 */
    data object Registering : ConnectionState

    /** 登録済みで、どのホストともつながっていない。 */
    data object Ready : ConnectionState

    data class Connecting(val host: HostDevice) : ConnectionState

    data class Connected(val host: HostDevice) : ConnectionState

    data class Error(val reason: ErrorReason) : ConnectionState
}

/**
 * 接続先の PC。
 *
 * @property address Bluetooth アドレス。前回のホストとして保存し、再接続に使う。
 * @property name 表示名(ホスト側の Bluetooth 名)。取得できない場合は null。機器名はハードコードしない。
 */
data class HostDevice(val address: String, val name: String?)

/** 使用中のエラーの種類。文言と回復方法は docs/design/v2.0-standard.md 7.5 の表のとおり。 */
enum class ErrorReason {
    /** 接続が切れた(再接続を試みる)。 */
    CONNECTION_LOST,

    /** 使用中に Bluetooth がオフになった。オンになったら再接続する。 */
    BLUETOOTH_OFF,

    /** PC が見つからない。 */
    HOST_NOT_FOUND,

    /** PC が接続を拒否した(ペアリングのやり直しが必要)。 */
    HOST_REJECTED,

    /** HID アプリの登録に失敗した。 */
    REGISTER_FAILED,
}
