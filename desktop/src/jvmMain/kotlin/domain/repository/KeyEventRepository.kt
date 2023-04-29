package domain.repository

import java.awt.Robot

class KeyEventRepository(
    private val robot: Robot = Robot()
) {
    fun keyPress(code: Int) {
        robot.keyPress(code)
    }

    fun keyRelease(code: Int) {
        robot.keyRelease(code)
    }
}