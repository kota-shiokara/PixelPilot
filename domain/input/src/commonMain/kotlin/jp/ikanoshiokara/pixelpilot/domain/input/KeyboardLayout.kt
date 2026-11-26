package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage
import jp.ikanoshiokara.pixelpilot.model.settings.KeyboardLayoutType

/**
 * ホストのキーボード配列(docs/design/v2.0-standard.md 5.1)。文字とキー(Usage と修飾キー)を対応づける。
 *
 * HID は物理的なキーの位置を送るため、ホストの配列に合わせて文字をキーに変換する。
 * 例: `@` は US 配列では Shift + `2`(0x1F)、JIS 配列では `@` キー(0x2F)。
 *
 * - IME 入力・テキスト送信(v2.x): [strokeOf] で文字をキー操作に変換する。
 * - アプリ内のキー配列: [keyRows] と [labelOf] でホスト配列に合わせたキーを描き、[symbols] で記号の並びを出す。
 *
 * 実装は [of] で取得する。
 */
interface KeyboardLayout {
    /** 配列の種類。 */
    val type: KeyboardLayoutType

    /**
     * 文字を入力するキーの並び。上の段(数字の段)から順に、各段を左から右へ並べる。スペースは含めない。
     * 入力切り替えのキー(JIS の半角/全角など)も含めない。それらは [imeKeys] にある。
     */
    val keyRows: List<List<Int>>

    /**
     * 記号(ASCII の英数字・空白以外の印字可能文字)の並び。[keyRows] のキー順に、各キーの Shift なし → Shift ありの順。
     * アプリ内のキー配列の「記号」で、ホスト配列と同じ並びで記号を出すのに使う。どの文字も [strokeOf] で入力できる。
     */
    val symbols: List<Char>

    /**
     * 配列に固有の、入力切り替えのキーの Usage。左から右の順。
     * JIS: 半角/全角(0x35)・無変換(International5, 0x8B)・変換(International4, 0x8A)・かな(International2, 0x88)。US: なし。
     *
     * macOS の英数 / かな(LANG2 0x91 / LANG1 0x90)は配列ではなくホストの OS で決まるため、ここには含めない。
     */
    val imeKeys: List<Int>

    /** 文字を (Usage, 修飾キー) に変換する。配列で入力できない文字は null。改行(`\n`)は Enter、タブ(`\t`)は Tab。 */
    fun strokeOf(c: Char): KeyStroke?

    /** [keyRows] にあるキーの刻印。それ以外のキーは null。 */
    fun labelOf(usage: Int): KeyLabel?

    companion object {
        /** [type] の配列を返す。 */
        fun of(type: KeyboardLayoutType): KeyboardLayout = when (type) {
            KeyboardLayoutType.US -> UsKeyboardLayout
            KeyboardLayoutType.JIS -> JisKeyboardLayout
        }
    }
}

/**
 * キーの刻印。アプリ内のキー配列のラベルに使う。
 *
 * 英字は [normal] が小文字、[shifted] が大文字。Caps Lock の状態による切り替えは表示側で行う。
 *
 * @property normal Shift なしで入力される文字の表示。
 * @property shifted Shift ありで入力される文字の表示。何も入力されないキー(JIS の `0` など)は null。
 */
data class KeyLabel(val normal: String, val shifted: String?)

/**
 * 1 つのキーの定義。
 *
 * @property normal Shift なしで入力される文字。[strokeOf] の対象にしない場合は null。
 * @property shifted Shift ありで入力される文字。無い場合は null。
 * @property label 刻印。入力される文字と表示が異なるキー(JIS の `¥`)で指定する。
 */
private class KeyDef(
    val usage: Int,
    val normal: Char?,
    val shifted: Char?,
    label: KeyLabel? = null,
) {
    val label: KeyLabel = label ?: KeyLabel(requireNotNull(normal) { "刻印が無い" }.toString(), shifted?.toString())
}

/** [KeyDef] の表から [KeyboardLayout] を作る。 */
private abstract class TableKeyboardLayout(
    override val type: KeyboardLayoutType,
    rows: List<List<KeyDef>>,
    override val imeKeys: List<Int>,
) : KeyboardLayout {
    override val keyRows: List<List<Int>> = rows.map { row -> row.map { it.usage } }

    private val labels: Map<Int, KeyLabel> = rows.flatten().associate { it.usage to it.label }

    private val strokes: Map<Char, KeyStroke> = buildMap {
        rows.flatten().forEach { key ->
            key.normal?.let { putUnique(it, KeyStroke.plain(key.usage)) }
            key.shifted?.let { putUnique(it, KeyStroke.shifted(key.usage)) }
        }
        putUnique(' ', KeyStroke.plain(KeyboardUsage.SPACE))
        putUnique('\n', KeyStroke.plain(KeyboardUsage.ENTER))
        putUnique('\t', KeyStroke.plain(KeyboardUsage.TAB))
    }

    override val symbols: List<Char> = rows.flatten()
        .flatMap { listOfNotNull(it.normal, it.shifted) }
        .filter { !it.isLetterOrDigit() }

    override fun strokeOf(c: Char): KeyStroke? = strokes[c]

    override fun labelOf(usage: Int): KeyLabel? = labels[usage]

    private fun MutableMap<Char, KeyStroke>.putUnique(c: Char, stroke: KeyStroke) {
        check(put(c, stroke) == null) { "$type 配列で '$c' が複数のキーに割り当てられている" }
    }
}

/** 英字キー(Shift なしで小文字、Shift ありで大文字)。 */
private fun letters(chars: String): List<KeyDef> = chars.map { c ->
    KeyDef(KeyboardUsage.A + (c - 'a'), c, c.uppercaseChar())
}

/** 数字の段の `1`〜`0`。[shifted] は `1`〜`0` の各キーの Shift ありの文字(無い場合は空白)。 */
private fun digits(shifted: String): List<KeyDef> {
    require(shifted.length == 10)
    return "1234567890".mapIndexed { i, c ->
        KeyDef(KeyboardUsage.DIGIT_1 + i, c, shifted[i].takeIf { it != ' ' })
    }
}

/** US 配列(ANSI)。 */
private object UsKeyboardLayout : TableKeyboardLayout(
    type = KeyboardLayoutType.US,
    rows = listOf(
        listOf(KeyDef(KeyboardUsage.GRAVE, '`', '~')) +
            digits("!@#$%^&*()") +
            listOf(
                KeyDef(KeyboardUsage.MINUS, '-', '_'),
                KeyDef(KeyboardUsage.EQUAL, '=', '+'),
            ),
        letters("qwertyuiop") + listOf(
            KeyDef(KeyboardUsage.LEFT_BRACKET, '[', '{'),
            KeyDef(KeyboardUsage.RIGHT_BRACKET, ']', '}'),
            KeyDef(KeyboardUsage.BACKSLASH, '\\', '|'),
        ),
        letters("asdfghjkl") + listOf(
            KeyDef(KeyboardUsage.SEMICOLON, ';', ':'),
            KeyDef(KeyboardUsage.APOSTROPHE, '\'', '"'),
        ),
        letters("zxcvbnm") + listOf(
            KeyDef(KeyboardUsage.COMMA, ',', '<'),
            KeyDef(KeyboardUsage.PERIOD, '.', '>'),
            KeyDef(KeyboardUsage.SLASH, '/', '?'),
        ),
    ),
    imeKeys = emptyList(),
)

/**
 * JIS 配列(日本語 106 / 109 キー)。対応は Windows・Linux(jp106)の日本語キーボード設定に合わせる。
 *
 * - `\` は `ろ` キー(International1)で入力する。`¥` キー(International3)は Windows では `\` になるが、
 *   macOS では `¥` になるため、`\` の入力には使わない。`¥` キーは `|`(Shift あり)の入力にだけ使う。
 * - Shift + `0` は何も入力されない。
 */
private object JisKeyboardLayout : TableKeyboardLayout(
    type = KeyboardLayoutType.JIS,
    rows = listOf(
        digits("!\"#$%&'() ") + listOf(
            KeyDef(KeyboardUsage.MINUS, '-', '='),
            KeyDef(KeyboardUsage.EQUAL, '^', '~'),
            KeyDef(KeyboardUsage.INTERNATIONAL_3, null, '|', label = KeyLabel("¥", "|")),
        ),
        letters("qwertyuiop") + listOf(
            KeyDef(KeyboardUsage.LEFT_BRACKET, '@', '`'),
            KeyDef(KeyboardUsage.RIGHT_BRACKET, '[', '{'),
        ),
        letters("asdfghjkl") + listOf(
            KeyDef(KeyboardUsage.SEMICOLON, ';', '+'),
            KeyDef(KeyboardUsage.APOSTROPHE, ':', '*'),
            KeyDef(KeyboardUsage.NON_US_HASH, ']', '}'),
        ),
        letters("zxcvbnm") + listOf(
            KeyDef(KeyboardUsage.COMMA, ',', '<'),
            KeyDef(KeyboardUsage.PERIOD, '.', '>'),
            KeyDef(KeyboardUsage.SLASH, '/', '?'),
            KeyDef(KeyboardUsage.INTERNATIONAL_1, '\\', '_'),
        ),
    ),
    imeKeys = listOf(
        KeyboardUsage.GRAVE, // 半角/全角
        KeyboardUsage.INTERNATIONAL_5, // 無変換
        KeyboardUsage.INTERNATIONAL_4, // 変換
        KeyboardUsage.INTERNATIONAL_2, // カタカナ/ひらがな
    ),
)
