package domain.usecase

import domain.repository.KeyEventRepository
import jp.ikanoshiokara.common.domain.model.command.KeyEvent

class KeyEventUseCase(
    private val keyEventRepository: KeyEventRepository
) {
    operator fun invoke(event: KeyEvent) {
        when(event) {
            is KeyEvent.Default -> return
            is KeyEvent.KeyPress -> {
                keyEventRepository.keyPress(event.keyCode.code)
            }
            is KeyEvent.KeyRelease -> {
                keyEventRepository.keyRelease(event.keyCode.code)
            }
        }
    }
}