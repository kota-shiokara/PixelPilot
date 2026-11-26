package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.Button

/**
 * 長押しで出る操作ダイアルの項目(#98)。指をずらす方向で選ぶ。
 *
 * @property button 選んで離したときに押して離すマウスボタン。
 */
enum class DialItem(val button: Button) {
    /** 左へずらす。 */
    BACK(Button.BACK),

    /** 右へずらす。 */
    FORWARD(Button.FORWARD),

    /** 上へずらす。 */
    MIDDLE(Button.MIDDLE),

    /** 下へずらす。 */
    RIGHT(Button.RIGHT),
}

/**
 * 開いている操作ダイアルの状態。表示は view が受け持つ。座標は [TouchEvent] と同じ dp。
 *
 * @property centerX ダイアルの中心(長押しを始めた位置)。
 * @property fingerX 今の指の位置。
 * @property selected 選んでいる項目。中央付近(キャンセルの範囲)にいる間は null。
 */
data class DialState(
    val centerX: Float,
    val centerY: Float,
    val fingerX: Float,
    val fingerY: Float,
    val selected: DialItem?,
)
