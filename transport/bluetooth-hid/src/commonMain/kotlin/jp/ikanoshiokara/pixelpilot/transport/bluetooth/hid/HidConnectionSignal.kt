package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice

/**
 * Bluetooth HID の制御で起きた出来事。Android の API の結果をこれに置き換えて [reduce] に渡す。
 * 状態の遷移を Android に依存しない純 Kotlin にして、単体テストできるようにするため。
 */
internal sealed interface HidConnectionSignal {
    /** 権限が無い。 */
    data object PermissionMissing : HidConnectionSignal

    data object BluetoothTurnedOff : HidConnectionSignal

    /** HID 対応の確認と登録を始めた。 */
    data object RegisterStarted : HidConnectionSignal

    /** HID Device プロファイルが取得できない、または応答が無い。 */
    data object ProfileUnavailable : HidConnectionSignal

    /** `registerApp` が失敗した、または登録が外れた。 */
    data object RegisterFailed : HidConnectionSignal

    /** `onAppStatusChanged(registered = true)` */
    data object Registered : HidConnectionSignal

    data class HostConnecting(val host: HostDevice) : HidConnectionSignal

    data class HostConnected(val host: HostDevice) : HidConnectionSignal

    data class HostDisconnected(val host: HostDevice) : HidConnectionSignal

    /** 停止した(アプリがバックグラウンドに回った等)。次の開始まで準備中の扱いにする。 */
    data object Stopped : HidConnectionSignal
}

/** 現在の状態に出来事を反映した、次の状態。 */
internal fun reduce(state: ConnectionState, signal: HidConnectionSignal): ConnectionState = when (signal) {
    HidConnectionSignal.PermissionMissing -> ConnectionState.PermissionRequired

    // 接続中にオフになった場合は、使用中の画面にエラーとして出す(7.5)。それ以外はホスト選択画面で案内する
    HidConnectionSignal.BluetoothTurnedOff -> when (state) {
        is ConnectionState.Connected, is ConnectionState.Connecting ->
            ConnectionState.Error(ErrorReason.BLUETOOTH_OFF)
        else -> ConnectionState.BluetoothOff
    }

    HidConnectionSignal.RegisterStarted -> ConnectionState.Registering

    HidConnectionSignal.ProfileUnavailable -> ConnectionState.Unsupported

    HidConnectionSignal.RegisterFailed -> when (state) {
        // 端末そのものの状態(非対応・権限・オフ)の方を優先して見せる
        ConnectionState.Unsupported, ConnectionState.PermissionRequired, ConnectionState.BluetoothOff -> state
        else -> ConnectionState.Error(ErrorReason.REGISTER_FAILED)
    }

    // 接続済みのまま登録の通知が来ても、接続中の表示は崩さない
    HidConnectionSignal.Registered -> when (state) {
        is ConnectionState.Connected, is ConnectionState.Connecting -> state
        else -> ConnectionState.Ready
    }

    is HidConnectionSignal.HostConnecting -> ConnectionState.Connecting(signal.host)

    is HidConnectionSignal.HostConnected -> ConnectionState.Connected(signal.host)

    is HidConnectionSignal.HostDisconnected -> when (state) {
        // 使っていたホストとの接続が切れた。再接続は #37 で行う
        is ConnectionState.Connected -> ConnectionState.Error(ErrorReason.CONNECTION_LOST)
        // 接続しようとしていたホストにつながらなかった
        is ConnectionState.Connecting -> ConnectionState.Error(ErrorReason.HOST_NOT_FOUND)
        else -> state
    }

    HidConnectionSignal.Stopped -> ConnectionState.Registering
}
