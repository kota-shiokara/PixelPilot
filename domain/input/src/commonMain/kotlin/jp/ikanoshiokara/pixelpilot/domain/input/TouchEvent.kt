package jp.ikanoshiokara.pixelpilot.domain.input

/**
 * タッチパッドに入ってくるタッチの出来事。UI(Compose の pointerInput など)から変換して [TouchpadInterpreter] に渡す。
 *
 * 座標は dp(端末の画面密度によらない単位)。時刻はミリ秒(単調増加していればよい)。
 */
sealed interface TouchEvent {
    val timeMillis: Long

    /** 指が触れた。[pointerId] は指ごとに異なる。 */
    data class Down(val pointerId: Long, val x: Float, val y: Float, override val timeMillis: Long) : TouchEvent

    /** 指が動いた。 */
    data class Move(val pointerId: Long, val x: Float, val y: Float, override val timeMillis: Long) : TouchEvent

    /** 指が離れた。 */
    data class Up(val pointerId: Long, override val timeMillis: Long) : TouchEvent

    /** システムにタッチを取られた(通知の引き出しなど)。進行中のジェスチャーを打ち切る。 */
    data class Cancel(override val timeMillis: Long) : TouchEvent
}
