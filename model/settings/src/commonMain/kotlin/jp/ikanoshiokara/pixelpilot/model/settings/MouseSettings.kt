package jp.ikanoshiokara.pixelpilot.model.settings

/**
 * マウス(タッチパッドの操作)の設定。docs/design/v2.0-standard.md 4.2。
 *
 * @property sensitivity 移動感度。1.0 が標準。[SENSITIVITY_RANGE] の範囲で設定する。
 * @property scrollDirection 2 本指でなぞったときのスクロールの向き。
 * @property showButtons 画面下部のクリックボタンを出すか。既定は出さない(13. 決定事項 18)。
 *   出さない場合も、クリック(左・右・中)と左ボタンのドラッグはタップ・操作ダイアル・タップ直後のドラッグで行える。
 *   右・中ボタンを押したままの操作(パンなど)はボタンでしかできないため、そのために表示する。
 */
data class MouseSettings(
    val sensitivity: Float = 1f,
    val scrollDirection: ScrollDirection = ScrollDirection.NATURAL,
    val showButtons: Boolean = false,
) {
    companion object {
        /** 設定画面で選べる移動感度の範囲。 */
        val SENSITIVITY_RANGE: ClosedFloatingPointRange<Float> = 0.5f..2f
    }
}

/** スクロールの向き。 */
enum class ScrollDirection {
    /** 指の動きに画面の内容がついてくる(指を上へ → 内容が上へ動く = 下へスクロール)。スマホと同じ。 */
    NATURAL,

    /** 指の動きと逆(指を上へ → 上へスクロール)。従来のマウスホイールと同じ。 */
    TRADITIONAL,
}
