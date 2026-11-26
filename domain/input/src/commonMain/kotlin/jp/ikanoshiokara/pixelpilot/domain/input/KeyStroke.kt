package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage

/**
 * 1 文字ぶんのキー操作。[usage] のキーを、[modifiers] の修飾キーを押したまま押す。
 *
 * @property usage Keyboard/Keypad Page(0x07)の Usage。修飾キーではないキー。
 * @property modifiers 同時に押す修飾キーの Usage(0xE0〜0xE7)。
 */
data class KeyStroke(
    val usage: Int,
    val modifiers: Set<Int> = emptySet(),
) {
    init {
        require(!KeyboardUsage.isModifier(usage)) { "修飾キーは usage ではなく modifiers に入れる: 0x${usage.toString(16)}" }
        require(modifiers.all(KeyboardUsage::isModifier)) { "修飾キーではない Usage が modifiers にある: $modifiers" }
    }

    /**
     * このキー操作を送るためのイベント列。修飾キーを押す → キーを押す → キーを離す → 修飾キーを離す、の順。
     */
    fun toInputEvents(): List<InputEvent.Key> = buildList {
        modifiers.forEach { add(InputEvent.Key(it, pressed = true)) }
        add(InputEvent.Key(usage, pressed = true))
        add(InputEvent.Key(usage, pressed = false))
        modifiers.reversed().forEach { add(InputEvent.Key(it, pressed = false)) }
    }

    companion object {
        /** 修飾キーなしで [usage] を押す。 */
        fun plain(usage: Int): KeyStroke = KeyStroke(usage)

        /** 左 Shift を押したまま [usage] を押す。 */
        fun shifted(usage: Int): KeyStroke = KeyStroke(usage, setOf(KeyboardUsage.LEFT_SHIFT))
    }
}
