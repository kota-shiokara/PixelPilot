package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.InputEvent

/**
 * [InputEvent] を HID レポートに変換する。種類ごとのエンコーダに振り分け、それぞれが押下状態や端数を持つ。
 *
 * スレッドセーフではない。送信キュー(1 本のスレッド)から呼ぶ前提(docs/design/v2.0-standard.md 3.3)。
 */
class HidReportEncoder(
    private val mouse: MouseReportEncoder = MouseReportEncoder(),
    private val keyboard: KeyboardReportEncoder = KeyboardReportEncoder(),
) {
    /** イベントを反映し、送るべきレポートを順に返す。送るものが無い場合は空のリスト。 */
    fun encode(event: InputEvent): List<HidReport> = when (event) {
        is InputEvent.PointerMove -> mouse.motion(dx = event.dx, dy = event.dy)
        is InputEvent.Scroll -> mouse.motion(dx = 0f, dy = 0f, wheel = event.vertical, pan = event.horizontal)
        is InputEvent.MouseButton -> mouse.button(event.button, event.pressed)
        is InputEvent.Key -> keyboard.key(event.usage, event.pressed)
        // Consumer Control(メディアキー)は v2.x(#52)で実装する
        is InputEvent.Consumer -> emptyList()
    }

    /**
     * 移動とスクロールをまとめて 1 つのレポートで送る。送信キューが一定間隔で蓄積分を吐き出すときに使う(3.3 送信)。
     */
    fun encodeMotion(dx: Float, dy: Float, wheel: Float, pan: Float): List<HidReport> =
        mouse.motion(dx, dy, wheel, pan)

    /** すべての状態を初期化する(切断時など)。押されたままのボタン・キーがあれば、離したレポートを返す。 */
    fun reset(): List<HidReport> = keyboard.reset() + mouse.reset()
}
