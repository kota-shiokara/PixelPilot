package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage

/**
 * キーボードのレポート(Report ID 1、8 バイト)を生成する。押下中の修飾キーとキーを状態として持つ。
 *
 * レポート: `[修飾キー][予約][キー1]…[キー6]`
 *
 * - 修飾キー(Left Control〜Right GUI)は 1 バイト目のビットで送り、キー欄には入れない。
 * - キー欄には押した順に最大 6 個を入れる。7 個以上を同時に押している間は、キー欄すべてを
 *   [KeyboardUsage.ERROR_ROLL_OVER] にする(HID の仕様。どのキーが押されているか分からないことをホストに伝える)。
 * - スレッドセーフではない。送信キュー(1 本のスレッド)から呼ぶ前提。
 */
class KeyboardReportEncoder {
    private var modifiers = 0
    private val pressedKeys = mutableListOf<Int>()

    /**
     * キーの押下・解放を反映したレポートを返す。状態が変わらない場合(押下中のキーをもう一度押した等)は空のリスト。
     *
     * @param usage Keyboard/Keypad Page の Usage(修飾キーを含む)
     */
    fun key(usage: Int, pressed: Boolean): List<HidReport> {
        require(usage in 0x01..0xFF) { "Keyboard/Keypad Page の範囲外の Usage: 0x${usage.toString(16)}" }
        val changed = if (KeyboardUsage.isModifier(usage)) {
            updateModifier(usage, pressed)
        } else {
            updateKey(usage, pressed)
        }
        return if (changed) listOf(report()) else emptyList()
    }

    /** 状態を初期化する(切断時など)。押されたままのキーがあれば、すべて離したレポートを返す。 */
    fun reset(): List<HidReport> {
        if (modifiers == 0 && pressedKeys.isEmpty()) return emptyList()
        modifiers = 0
        pressedKeys.clear()
        return listOf(report())
    }

    private fun updateModifier(usage: Int, pressed: Boolean): Boolean {
        val bit = KeyboardUsage.modifierBit(usage)
        val next = if (pressed) modifiers or bit else modifiers and bit.inv()
        if (next == modifiers) return false
        modifiers = next
        return true
    }

    private fun updateKey(usage: Int, pressed: Boolean): Boolean =
        if (pressed) {
            if (usage in pressedKeys) false else pressedKeys.add(usage)
        } else {
            pressedKeys.remove(usage)
        }

    private fun report(): HidReport {
        val data = ByteArray(ReportLength.KEYBOARD_INPUT)
        data[0] = modifiers.toByte()
        // data[1] は予約(0)
        if (pressedKeys.size > MAX_KEYS) {
            for (i in 0 until MAX_KEYS) data[2 + i] = KeyboardUsage.ERROR_ROLL_OVER.toByte()
        } else {
            pressedKeys.forEachIndexed { i, usage -> data[2 + i] = usage.toByte() }
        }
        return HidReport(ReportId.KEYBOARD, data)
    }

    private companion object {
        const val MAX_KEYS = 6
    }
}
