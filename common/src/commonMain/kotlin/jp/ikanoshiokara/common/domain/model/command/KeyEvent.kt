package jp.ikanoshiokara.common.domain.model.command

sealed class KeyEvent {
    object Default: KeyEvent()
    object KeyPress: KeyEvent()
    object KeyRelease: KeyEvent()
}
