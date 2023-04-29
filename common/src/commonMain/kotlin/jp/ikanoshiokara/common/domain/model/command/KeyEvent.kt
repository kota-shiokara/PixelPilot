package jp.ikanoshiokara.common.domain.model.command

sealed class KeyEvent {
    object Default: KeyEvent()
    data class KeyPress(val keyCode: KeyCode): KeyEvent()
    data class KeyRelease(val keyCode: KeyCode): KeyEvent()
}
