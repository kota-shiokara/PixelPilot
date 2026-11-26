package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage
import jp.ikanoshiokara.pixelpilot.model.settings.KeyboardLayoutType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** ASCII 全文字(0x00〜0x7F)の変換表を、US / JIS それぞれ明示した期待値と突き合わせる(docs/design/v2.0-standard.md 11.)。 */
class KeyboardLayoutTest {
    private val us = KeyboardLayout.of(KeyboardLayoutType.US)
    private val jis = KeyboardLayout.of(KeyboardLayoutType.JIS)

    /** 英字・数字・空白・改行・タブ。US / JIS で共通。 */
    private val common: Map<Char, KeyStroke> = buildMap {
        ('a'..'z').forEachIndexed { i, c ->
            put(c, KeyStroke.plain(0x04 + i))
            put(c.uppercaseChar(), KeyStroke.shifted(0x04 + i))
        }
        "1234567890".forEachIndexed { i, c -> put(c, KeyStroke.plain(0x1E + i)) }
        put(' ', KeyStroke.plain(0x2C))
        put('\n', KeyStroke.plain(0x28))
        put('\t', KeyStroke.plain(0x2B))
    }

    private val usExpected: Map<Char, KeyStroke> = common + mapOf(
        '!' to KeyStroke.shifted(0x1E),
        '@' to KeyStroke.shifted(0x1F),
        '#' to KeyStroke.shifted(0x20),
        '$' to KeyStroke.shifted(0x21),
        '%' to KeyStroke.shifted(0x22),
        '^' to KeyStroke.shifted(0x23),
        '&' to KeyStroke.shifted(0x24),
        '*' to KeyStroke.shifted(0x25),
        '(' to KeyStroke.shifted(0x26),
        ')' to KeyStroke.shifted(0x27),
        '-' to KeyStroke.plain(0x2D),
        '_' to KeyStroke.shifted(0x2D),
        '=' to KeyStroke.plain(0x2E),
        '+' to KeyStroke.shifted(0x2E),
        '[' to KeyStroke.plain(0x2F),
        '{' to KeyStroke.shifted(0x2F),
        ']' to KeyStroke.plain(0x30),
        '}' to KeyStroke.shifted(0x30),
        '\\' to KeyStroke.plain(0x31),
        '|' to KeyStroke.shifted(0x31),
        ';' to KeyStroke.plain(0x33),
        ':' to KeyStroke.shifted(0x33),
        '\'' to KeyStroke.plain(0x34),
        '"' to KeyStroke.shifted(0x34),
        '`' to KeyStroke.plain(0x35),
        '~' to KeyStroke.shifted(0x35),
        ',' to KeyStroke.plain(0x36),
        '<' to KeyStroke.shifted(0x36),
        '.' to KeyStroke.plain(0x37),
        '>' to KeyStroke.shifted(0x37),
        '/' to KeyStroke.plain(0x38),
        '?' to KeyStroke.shifted(0x38),
    )

    private val jisExpected: Map<Char, KeyStroke> = common + mapOf(
        '!' to KeyStroke.shifted(0x1E),
        '"' to KeyStroke.shifted(0x1F),
        '#' to KeyStroke.shifted(0x20),
        '$' to KeyStroke.shifted(0x21),
        '%' to KeyStroke.shifted(0x22),
        '&' to KeyStroke.shifted(0x23),
        '\'' to KeyStroke.shifted(0x24),
        '(' to KeyStroke.shifted(0x25),
        ')' to KeyStroke.shifted(0x26),
        '-' to KeyStroke.plain(0x2D),
        '=' to KeyStroke.shifted(0x2D),
        '^' to KeyStroke.plain(0x2E),
        '~' to KeyStroke.shifted(0x2E),
        '|' to KeyStroke.shifted(0x89), // ¥ キー(International3)
        '@' to KeyStroke.plain(0x2F),
        '`' to KeyStroke.shifted(0x2F),
        '[' to KeyStroke.plain(0x30),
        '{' to KeyStroke.shifted(0x30),
        ';' to KeyStroke.plain(0x33),
        '+' to KeyStroke.shifted(0x33),
        ':' to KeyStroke.plain(0x34),
        '*' to KeyStroke.shifted(0x34),
        ']' to KeyStroke.plain(0x32),
        '}' to KeyStroke.shifted(0x32),
        ',' to KeyStroke.plain(0x36),
        '<' to KeyStroke.shifted(0x36),
        '.' to KeyStroke.plain(0x37),
        '>' to KeyStroke.shifted(0x37),
        '/' to KeyStroke.plain(0x38),
        '?' to KeyStroke.shifted(0x38),
        '\\' to KeyStroke.plain(0x87), // ろ キー(International1)
        '_' to KeyStroke.shifted(0x87),
    )

    private val printable = (0x20..0x7E).map { it.toChar() }

    @Test
    fun expectedTablesCoverAllPrintableAscii() {
        // 期待値の表そのものに漏れが無いことを確かめる。印字可能 95 文字 + 改行・タブ。
        assertEquals(printable.toSet() + setOf('\n', '\t'), usExpected.keys)
        assertEquals(printable.toSet() + setOf('\n', '\t'), jisExpected.keys)
    }

    @Test
    fun usConvertsAllAscii() = assertTable(us, usExpected)

    @Test
    fun jisConvertsAllAscii() = assertTable(jis, jisExpected)

    private fun assertTable(layout: KeyboardLayout, expected: Map<Char, KeyStroke>) {
        (0x00..0x7F).map { it.toChar() }.forEach { c ->
            assertEquals(expected[c], layout.strokeOf(c), "${layout.type} 0x${c.code.toString(16)} '$c'")
        }
    }

    @Test
    fun nonAsciiIsNotTypable() {
        listOf('¥', 'あ', 'é', '\r', '\u007F').forEach { c ->
            assertNull(us.strokeOf(c), "US '$c'")
            assertNull(jis.strokeOf(c), "JIS '$c'")
        }
    }

    @Test
    fun ofReturnsLayoutOfType() {
        assertEquals(KeyboardLayoutType.US, us.type)
        assertEquals(KeyboardLayoutType.JIS, jis.type)
    }

    @Test
    fun symbolsAreAllAsciiSymbolsInKeyOrder() {
        val asciiSymbols = printable.filter { !it.isLetterOrDigit() && it != ' ' }.toSet()
        listOf(us, jis).forEach { layout ->
            assertEquals(32, layout.symbols.size, "${layout.type}")
            assertEquals(asciiSymbols, layout.symbols.toSet(), "${layout.type}")
            layout.symbols.forEach { c -> requireNotNull(layout.strokeOf(c)) { "${layout.type} '$c'" } }
        }
        assertEquals("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?", us.symbols.joinToString(""))
        assertEquals("!\"#$%&'()-=^~|@`[{;+:*]},<.>/?\\_", jis.symbols.joinToString(""))
    }

    @Test
    fun keyRowsFollowPhysicalLayout() {
        assertEquals(
            listOf(
                listOf(0x35, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x2D, 0x2E),
                listOf(0x14, 0x1A, 0x08, 0x15, 0x17, 0x1C, 0x18, 0x0C, 0x12, 0x13, 0x2F, 0x30, 0x31),
                listOf(0x04, 0x16, 0x07, 0x09, 0x0A, 0x0B, 0x0D, 0x0E, 0x0F, 0x33, 0x34),
                listOf(0x1D, 0x1B, 0x06, 0x19, 0x05, 0x11, 0x10, 0x36, 0x37, 0x38),
            ),
            us.keyRows,
        )
        assertEquals(
            listOf(
                listOf(0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x2D, 0x2E, 0x89),
                listOf(0x14, 0x1A, 0x08, 0x15, 0x17, 0x1C, 0x18, 0x0C, 0x12, 0x13, 0x2F, 0x30),
                listOf(0x04, 0x16, 0x07, 0x09, 0x0A, 0x0B, 0x0D, 0x0E, 0x0F, 0x33, 0x34, 0x32),
                listOf(0x1D, 0x1B, 0x06, 0x19, 0x05, 0x11, 0x10, 0x36, 0x37, 0x38, 0x87),
            ),
            jis.keyRows,
        )
    }

    @Test
    fun labelsFollowHostLayout() {
        assertEquals(KeyLabel("2", "@"), us.labelOf(KeyboardUsage.DIGIT_2))
        assertEquals(KeyLabel("2", "\""), jis.labelOf(KeyboardUsage.DIGIT_2))
        assertEquals(KeyLabel("0", null), jis.labelOf(KeyboardUsage.DIGIT_0))
        assertEquals(KeyLabel("[", "{"), us.labelOf(KeyboardUsage.LEFT_BRACKET))
        assertEquals(KeyLabel("@", "`"), jis.labelOf(KeyboardUsage.LEFT_BRACKET))
        assertEquals(KeyLabel("¥", "|"), jis.labelOf(KeyboardUsage.INTERNATIONAL_3))
        assertEquals(KeyLabel("\\", "_"), jis.labelOf(KeyboardUsage.INTERNATIONAL_1))
        assertEquals(KeyLabel("a", "A"), us.labelOf(KeyboardUsage.A))
        // 文字を入力しないキー・その配列に無いキー
        assertNull(us.labelOf(KeyboardUsage.ENTER))
        assertNull(us.labelOf(KeyboardUsage.INTERNATIONAL_3))
        assertNull(jis.labelOf(KeyboardUsage.GRAVE))
        assertNull(jis.labelOf(KeyboardUsage.BACKSLASH))
    }

    @Test
    fun everyKeyInRowsHasLabel() {
        listOf(us, jis).forEach { layout ->
            layout.keyRows.flatten().forEach { usage ->
                requireNotNull(layout.labelOf(usage)) { "${layout.type} 0x${usage.toString(16)}" }
            }
        }
    }

    @Test
    fun imeKeysOfJis() {
        assertEquals(listOf(0x35, 0x8B, 0x8A, 0x88), jis.imeKeys)
        assertEquals(emptyList<Int>(), us.imeKeys)
    }

    @Test
    fun strokeToInputEventsWrapsKeyWithModifiers() {
        assertEquals(
            listOf(
                InputEvent.Key(KeyboardUsage.LEFT_SHIFT, pressed = true),
                InputEvent.Key(KeyboardUsage.DIGIT_2, pressed = true),
                InputEvent.Key(KeyboardUsage.DIGIT_2, pressed = false),
                InputEvent.Key(KeyboardUsage.LEFT_SHIFT, pressed = false),
            ),
            us.strokeOf('@')!!.toInputEvents(),
        )
        assertEquals(
            listOf(
                InputEvent.Key(KeyboardUsage.LEFT_BRACKET, pressed = true),
                InputEvent.Key(KeyboardUsage.LEFT_BRACKET, pressed = false),
            ),
            jis.strokeOf('@')!!.toInputEvents(),
        )
    }

    @Test
    fun keyStrokeRejectsModifierAsUsage() {
        assertFailsWith<IllegalArgumentException> { KeyStroke(KeyboardUsage.LEFT_SHIFT) }
        assertFailsWith<IllegalArgumentException> { KeyStroke(KeyboardUsage.A, setOf(KeyboardUsage.B)) }
    }
}
