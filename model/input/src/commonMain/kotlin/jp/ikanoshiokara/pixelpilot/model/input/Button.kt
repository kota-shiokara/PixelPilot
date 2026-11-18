package jp.ikanoshiokara.pixelpilot.model.input

/**
 * マウスボタン。
 *
 * @property usage Button Page(0x09)の Usage。マウスのレポートでは `usage - 1` 番目のビットになる。
 */
enum class Button(val usage: Int) {
    LEFT(1),
    RIGHT(2),
    MIDDLE(3),
    BACK(4),
    FORWARD(5),
}
