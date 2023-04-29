package jp.ikanoshiokara.common.domain.model.command

data class Command(
    val message: String = "",
    val keyEvent: KeyEvent = KeyEvent.Default
)
