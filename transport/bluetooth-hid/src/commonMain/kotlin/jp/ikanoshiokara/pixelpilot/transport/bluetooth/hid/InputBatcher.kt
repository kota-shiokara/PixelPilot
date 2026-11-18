package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import jp.ikanoshiokara.pixelpilot.domain.hid.HidReport
import jp.ikanoshiokara.pixelpilot.domain.hid.HidReportEncoder
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent

/**
 * 送信キューで、移動・スクロールをまとめてから送るための溜め置き(docs/design/v2.0-standard.md 3.3)。
 *
 * - 移動・スクロールは溜めておき、[flush] でまとめて 1 つのレポートにする(一定間隔で呼ぶ)。
 * - ボタン・キーなどはすぐに送る。その前に溜めている移動を吐き出し、操作の順序を保つ
 *   (例: 移動してからクリックしたのに、クリックが先に届くことがないように)。
 *
 * 純 Kotlin。スレッドセーフではなく、送信キューの 1 本のスレッドから呼ぶ。
 */
internal class InputBatcher(private val encoder: HidReportEncoder = HidReportEncoder()) {
    private var dx = 0f
    private var dy = 0f
    private var wheel = 0f
    private var pan = 0f

    /** まだ送っていない移動・スクロールがあるか。 */
    val hasPendingMotion: Boolean
        get() = dx != 0f || dy != 0f || wheel != 0f || pan != 0f

    /** イベントを受け取り、今すぐ送るべきレポートを返す。移動・スクロールは溜めるだけで空のリストを返す。 */
    fun add(event: InputEvent): List<HidReport> = when (event) {
        is InputEvent.PointerMove -> {
            dx += event.dx
            dy += event.dy
            emptyList()
        }
        is InputEvent.Scroll -> {
            wheel += event.vertical
            pan += event.horizontal
            emptyList()
        }
        else -> flush() + encoder.encode(event)
    }

    /** 溜めている移動・スクロールを、まとめたレポートにする。端数はエンコーダが持ち越す。 */
    fun flush(): List<HidReport> {
        if (!hasPendingMotion) return emptyList()
        val reports = encoder.encodeMotion(dx, dy, wheel, pan)
        clearMotion()
        return reports
    }

    /** 溜めている移動を捨て、押下状態を初期化する(切断時)。押されたままだった分を離すレポートを返す。 */
    fun reset(): List<HidReport> {
        clearMotion()
        return encoder.reset()
    }

    private fun clearMotion() {
        dx = 0f
        dy = 0f
        wheel = 0f
        pan = 0f
    }
}
