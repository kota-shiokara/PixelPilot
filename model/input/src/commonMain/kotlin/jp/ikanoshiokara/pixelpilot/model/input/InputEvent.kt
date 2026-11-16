package jp.ikanoshiokara.pixelpilot.model.input

/**
 * 経路によらない共通の入力イベント。HID Usage で表現する。
 *
 * - Bluetooth HID 経路: HidReportEncoder(`domain/hid`)が HID レポートに変換して送る。
 * - デスクトップ版経由の経路(v2.x): デスクトップ版が OS の入力 API に変換する。
 *
 * 移動量・スクロール量は浮動小数で持つ。整数への丸めと端数の持ち越しはエンコーダが行う。
 */
sealed interface InputEvent {
    /**
     * カーソルの相対移動。
     *
     * @property dx 右が正。
     * @property dy 下が正(HID の Y 軸と同じ向き)。
     */
    data class PointerMove(val dx: Float, val dy: Float) : InputEvent

    /**
     * スクロール量(HID のホイールの段数)。スクロール方向の設定(ナチュラル / 従来)は、
     * このイベントを作る側(TouchpadInterpreter)で反映済みとする。
     *
     * @property vertical 上(画面の内容が下へ動く向き)が正。HID の Wheel と同じ向き。
     * @property horizontal 右が正。HID の AC Pan と同じ向き。
     */
    data class Scroll(val vertical: Float, val horizontal: Float) : InputEvent

    /** マウスボタンの押下・解放。 */
    data class MouseButton(val button: Button, val pressed: Boolean) : InputEvent

    /**
     * キーの押下・解放。
     *
     * @property usage Keyboard/Keypad Page(0x07)の Usage。修飾キー(0xE0〜0xE7)も含む。[KeyboardUsage] を参照。
     */
    data class Key(val usage: Int, val pressed: Boolean) : InputEvent

    /**
     * メディアキーなどの押下・解放。
     *
     * @property usage Consumer Page(0x0C)の Usage。[ConsumerUsage] を参照。
     */
    data class Consumer(val usage: Int, val pressed: Boolean) : InputEvent
}
