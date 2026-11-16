package jp.ikanoshiokara.pixelpilot.model.input

/**
 * Keyboard/Keypad Page(0x07)の Usage。値は USB HID Usage Tables に従う。
 *
 * HID は物理的なキーの位置を送るため、同じ Usage でもホストの配列(US / JIS)によって入力される文字が変わる。
 * 文字との対応は KeyboardLayout(`domain/input`)で扱う。名前は US 配列での刻印に合わせ、JIS 配列での刻印をコメントに書く。
 */
object KeyboardUsage {
    /** 7 キー以上が同時に押されたときに、キー欄すべてに入れる値。 */
    const val ERROR_ROLL_OVER = 0x01

    const val A = 0x04
    const val B = 0x05
    const val C = 0x06
    const val D = 0x07
    const val E = 0x08
    const val F = 0x09
    const val G = 0x0A
    const val H = 0x0B
    const val I = 0x0C
    const val J = 0x0D
    const val K = 0x0E
    const val L = 0x0F
    const val M = 0x10
    const val N = 0x11
    const val O = 0x12
    const val P = 0x13
    const val Q = 0x14
    const val R = 0x15
    const val S = 0x16
    const val T = 0x17
    const val U = 0x18
    const val V = 0x19
    const val W = 0x1A
    const val X = 0x1B
    const val Y = 0x1C
    const val Z = 0x1D

    const val DIGIT_1 = 0x1E
    const val DIGIT_2 = 0x1F
    const val DIGIT_3 = 0x20
    const val DIGIT_4 = 0x21
    const val DIGIT_5 = 0x22
    const val DIGIT_6 = 0x23
    const val DIGIT_7 = 0x24
    const val DIGIT_8 = 0x25
    const val DIGIT_9 = 0x26
    const val DIGIT_0 = 0x27

    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C

    /** US: `-` / JIS: `-` */
    const val MINUS = 0x2D

    /** US: `=` / JIS: `^` */
    const val EQUAL = 0x2E

    /** US: `[` / JIS: `@` */
    const val LEFT_BRACKET = 0x2F

    /** US: `]` / JIS: `[` */
    const val RIGHT_BRACKET = 0x30

    /** US: `\` */
    const val BACKSLASH = 0x31

    /** JIS: `]`(US 配列には無い位置。Non-US # and ~) */
    const val NON_US_HASH = 0x32

    /** US: `;` / JIS: `;` */
    const val SEMICOLON = 0x33

    /** US: `'` / JIS: `:` */
    const val APOSTROPHE = 0x34

    /** US: `` ` `` / JIS: 半角/全角 */
    const val GRAVE = 0x35

    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val CAPS_LOCK = 0x39

    const val F1 = 0x3A
    const val F2 = 0x3B
    const val F3 = 0x3C
    const val F4 = 0x3D
    const val F5 = 0x3E
    const val F6 = 0x3F
    const val F7 = 0x40
    const val F8 = 0x41
    const val F9 = 0x42
    const val F10 = 0x43
    const val F11 = 0x44
    const val F12 = 0x45

    const val PRINT_SCREEN = 0x46
    const val SCROLL_LOCK = 0x47
    const val PAUSE = 0x48
    const val INSERT = 0x49
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT_ARROW = 0x4F
    const val LEFT_ARROW = 0x50
    const val DOWN_ARROW = 0x51
    const val UP_ARROW = 0x52

    /** Windows のメニューキー(Application) */
    const val APPLICATION = 0x65

    /** JIS: `ろ`(`\` `_`) */
    const val INTERNATIONAL_1 = 0x87

    /** JIS: カタカナ/ひらがな/ローマ字 */
    const val INTERNATIONAL_2 = 0x88

    /** JIS: `¥`(`|`) */
    const val INTERNATIONAL_3 = 0x89

    /** JIS: 変換 */
    const val INTERNATIONAL_4 = 0x8A

    /** JIS: 無変換 */
    const val INTERNATIONAL_5 = 0x8B

    /** macOS の「かな」 */
    const val LANG_1 = 0x90

    /** macOS の「英数」 */
    const val LANG_2 = 0x91

    const val LEFT_CONTROL = 0xE0
    const val LEFT_SHIFT = 0xE1
    const val LEFT_ALT = 0xE2

    /** Windows キー / ⌘ */
    const val LEFT_GUI = 0xE3

    const val RIGHT_CONTROL = 0xE4
    const val RIGHT_SHIFT = 0xE5
    const val RIGHT_ALT = 0xE6
    const val RIGHT_GUI = 0xE7

    /** 修飾キー(Left Control〜Right GUI)かどうか。修飾キーはレポートのキー欄ではなく修飾キーのビットで送る。 */
    fun isModifier(usage: Int): Boolean = usage in LEFT_CONTROL..RIGHT_GUI

    /**
     * 修飾キーのビット(レポートの 1 バイト目)。Left Control が bit 0、Right GUI が bit 7。
     *
     * @throws IllegalArgumentException 修飾キーでない場合
     */
    fun modifierBit(usage: Int): Int {
        require(isModifier(usage)) { "修飾キーではない Usage: 0x${usage.toString(16)}" }
        return 1 shl (usage - LEFT_CONTROL)
    }
}
