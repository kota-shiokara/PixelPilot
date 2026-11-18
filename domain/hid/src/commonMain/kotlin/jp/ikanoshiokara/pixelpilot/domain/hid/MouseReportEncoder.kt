package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.Button
import kotlin.math.abs
import kotlin.math.round

/**
 * マウスのレポート(Report ID 2、7 バイト)を生成する。押下中のボタンと、移動・スクロールの端数を状態として持つ。
 *
 * レポート: `[ボタン][X 下位][X 上位][Y 下位][Y 上位][ホイール][横スクロール]`(X / Y はリトルエンディアンの 16 ビット)
 *
 * - 移動・スクロールは浮動小数で蓄積し、整数部だけを送って端数は次に持ち越す。ゆっくり動かしても移動が失われない。
 * - 1 レポートに入らない量(X / Y は ±32767、ホイール・横スクロールは ±127 を超える分)は、複数のレポートに分けて送る。
 * - スレッドセーフではない。送信キュー(1 本のスレッド)から呼ぶ前提。
 *
 * @param wheelMultiplier ホイール・横スクロールの量に掛ける倍率。高解像度スクロール(v2.x)で
 * Resolution Multiplier を使う場合に、ホスト側の倍率に合わせて変える。v2.0 は 1。
 */
class MouseReportEncoder(private val wheelMultiplier: Int = 1) {
    init {
        require(wheelMultiplier >= 1) { "wheelMultiplier は 1 以上: $wheelMultiplier" }
    }

    private var buttons = 0
    private var remainderX = 0f
    private var remainderY = 0f
    private var remainderWheel = 0f
    private var remainderPan = 0f

    /**
     * 移動・スクロールを加え、送るべきレポートを返す。整数部が 0 の場合(端数だけの場合)は空のリストを返す。
     *
     * @param dx 右が正
     * @param dy 下が正
     * @param wheel 上が正(ホイールの段数)
     * @param pan 右が正(横スクロールの段数)
     */
    fun motion(dx: Float, dy: Float, wheel: Float = 0f, pan: Float = 0f): List<HidReport> {
        remainderX += dx
        remainderY += dy
        remainderWheel += wheel * wheelMultiplier
        remainderPan += pan * wheelMultiplier

        var x = wholePart(remainderX)
        var y = wholePart(remainderY)
        var w = wholePart(remainderWheel)
        var p = wholePart(remainderPan)
        remainderX -= x
        remainderY -= y
        remainderWheel -= w
        remainderPan -= p

        val reports = mutableListOf<HidReport>()
        while (x != 0 || y != 0 || w != 0 || p != 0) {
            val cx = x.coerceIn(-MAX_XY, MAX_XY)
            val cy = y.coerceIn(-MAX_XY, MAX_XY)
            val cw = w.coerceIn(-MAX_WHEEL, MAX_WHEEL)
            val cp = p.coerceIn(-MAX_WHEEL, MAX_WHEEL)
            reports += report(cx, cy, cw, cp)
            x -= cx
            y -= cy
            w -= cw
            p -= cp
        }
        return reports
    }

    /**
     * ボタンの押下・解放を反映したレポートを返す。状態が変わらない場合(押下中のボタンをもう一度押した等)は空のリスト。
     * 押下と解放は別のレポートとして送る(クリックは押下 → 解放の 2 レポート)。
     */
    fun button(button: Button, pressed: Boolean): List<HidReport> {
        val bit = 1 shl (button.usage - 1)
        val next = if (pressed) buttons or bit else buttons and bit.inv()
        if (next == buttons) return emptyList()
        buttons = next
        return listOf(report(0, 0, 0, 0))
    }

    /**
     * 状態を初期化する(切断時など)。ボタンが押されたままだった場合は、すべて離したレポートを返す。
     */
    fun reset(): List<HidReport> {
        remainderX = 0f
        remainderY = 0f
        remainderWheel = 0f
        remainderPan = 0f
        if (buttons == 0) return emptyList()
        buttons = 0
        return listOf(report(0, 0, 0, 0))
    }

    private fun report(x: Int, y: Int, wheel: Int, pan: Int): HidReport {
        val data = ByteArray(ReportLength.MOUSE_INPUT)
        data[0] = buttons.toByte()
        data[1] = (x and 0xFF).toByte()
        data[2] = ((x shr 8) and 0xFF).toByte()
        data[3] = (y and 0xFF).toByte()
        data[4] = ((y shr 8) and 0xFF).toByte()
        data[5] = wheel.toByte()
        data[6] = pan.toByte()
        return HidReport(ReportId.MOUSE, data)
    }

    /** 0 に向かって切り捨てた整数部。端数(小数部)は呼び出し側で持ち越す。 */
    private fun wholePart(value: Float): Int {
        // 浮動小数の誤差で 0.9999… のように 1 に届かない状態が続かないよう、整数にごく近い値は丸める
        val rounded = round(value)
        return if (abs(value - rounded) < EPSILON) rounded.toInt() else value.toInt()
    }

    private companion object {
        const val MAX_XY = 32767
        const val MAX_WHEEL = 127
        const val EPSILON = 1e-4f
    }
}
