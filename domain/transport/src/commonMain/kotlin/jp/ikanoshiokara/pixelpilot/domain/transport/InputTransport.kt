package jp.ikanoshiokara.pixelpilot.domain.transport

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardLeds
import kotlinx.coroutines.flow.StateFlow

/**
 * 入力イベントを PC に届ける経路。UI(view 配下)はこのインターフェース越しに送り、経路の実装を知らない。
 *
 * 実装: Bluetooth HID(`transport/bluetooth-hid`)。v2.x でデスクトップ版経由の経路を追加する。
 */
interface InputTransport {
    /** この経路の種類。 */
    val kind: TransportKind

    /** この経路の接続状態。UI(接続状態の表示など)はこれを描画する。 */
    val state: StateFlow<ConnectionState>

    /** ホストから通知されたキーボードの LED(Caps Lock など)。通知が無い経路では常に初期値。 */
    val keyboardLeds: StateFlow<KeyboardLeds>

    /**
     * イベントを送る。呼び出し元のスレッド(UI スレッドなど)をブロックしない。
     * 未接続など送れない状態のときは捨てる(押下中のキーやボタンは、再接続時に持ち越さない)。
     */
    fun send(event: InputEvent)
}
