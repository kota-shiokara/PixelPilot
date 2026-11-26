package jp.ikanoshiokara.pixelpilot.model.settings

/**
 * マウス(タッチパッドの操作)の設定。docs/design/v2.0-standard.md 4.2。
 *
 * @property sensitivity 移動感度。1.0 が標準。
 * @property scrollDirection 2 本指でなぞったときのスクロールの向き。
 * @property showButtons 画面下部のクリックボタンを出すか。出さない場合も、クリックはタップで行える。
 */
data class MouseSettings(
    val sensitivity: Float = 1f,
    val scrollDirection: ScrollDirection = ScrollDirection.NATURAL,
    val showButtons: Boolean = true,
)

/** スクロールの向き。 */
enum class ScrollDirection {
    /** 指の動きに画面の内容がついてくる(指を上へ → 内容が上へ動く = 下へスクロール)。スマホと同じ。 */
    NATURAL,

    /** 指の動きと逆(指を上へ → 上へスクロール)。従来のマウスホイールと同じ。 */
    TRADITIONAL,
}
