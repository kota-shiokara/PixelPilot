package jp.ikanoshiokara.common.domain.model.command

data class KeyCode(
    val code: Int
) {
    companion object {
        val DEFAULT = KeyCode(0)
        val LEFT = KeyCode(37)
        val UP = KeyCode(38)
        val RIGHT = KeyCode(39)
        val DOWN = KeyCode(40)
    }
}
