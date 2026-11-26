package jp.ikanoshiokara.pixelpilot.model.settings

/**
 * ホストのキーボード配列の種類。ホストごとに設定で選ぶ(docs/design/v2.0-standard.md 5.1)。
 *
 * HID は物理的なキーの位置を送るため、同じキーでもホストの配列によって入力される文字が変わる。
 * 文字とキーの対応は KeyboardLayout(`domain/input`)で扱う。
 */
enum class KeyboardLayoutType {
    /** US 配列(ANSI)。 */
    US,

    /** JIS 配列(日本語 106 / 109 キー)。 */
    JIS,
}
