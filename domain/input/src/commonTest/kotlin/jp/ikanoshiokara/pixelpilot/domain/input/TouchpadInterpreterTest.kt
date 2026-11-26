package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.Button
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TouchpadInterpreterTest {
    // 計算を分かりやすくするため、感度 1.0 で 1dp = 1 の移動、16dp でホイール 1 段にする
    private val config = TouchpadConfig(pointerGain = 1f, scrollDpPerStep = 16f)
    private val interpreter = TouchpadInterpreter(MouseSettings(scrollDirection = ScrollDirection.TRADITIONAL), config)

    private var now = 1_000L

    private fun down(id: Long, x: Float, y: Float) = interpreter.onTouch(TouchEvent.Down(id, x, y, now))
    private fun move(id: Long, x: Float, y: Float) = interpreter.onTouch(TouchEvent.Move(id, x, y, now))
    private fun up(id: Long) = interpreter.onTouch(TouchEvent.Up(id, now))
    private fun wait(millis: Long) { now += millis }

    private fun click(button: Button) = listOf(
        InputEvent.MouseButton(button, pressed = true),
        InputEvent.MouseButton(button, pressed = false),
    )

    @Test
    fun oneFingerTapIsLeftClick() {
        down(1, 100f, 100f)
        wait(80)
        assertEquals(click(Button.LEFT), up(1))
    }

    @Test
    fun smallJitterWithinSlopIsStillTap() {
        down(1, 100f, 100f)
        assertTrue(move(1, 103f, 104f).isEmpty())
        wait(80)
        assertEquals(click(Button.LEFT), up(1))
    }

    @Test
    fun longPressIsNotTap() {
        down(1, 100f, 100f)
        wait(600)
        assertTrue(up(1).isEmpty())
    }

    @Test
    fun oneFingerDragMovesPointerAfterSlop() {
        down(1, 100f, 100f)
        // 10dp 動いた時点でスロップ(8dp)を超える。超えるまでの分は送らない
        assertEquals(listOf(InputEvent.PointerMove(10f, 0f)), move(1, 110f, 100f))
        assertEquals(listOf(InputEvent.PointerMove(5f, -3f)), move(1, 115f, 97f))
        wait(80)
        // 動いた後は、短くてもタップにはしない
        assertTrue(up(1).isEmpty())
    }

    @Test
    fun sensitivityScalesPointerMovement() {
        interpreter.settings = interpreter.settings.copy(sensitivity = 2f)
        down(1, 0f, 0f)
        move(1, 10f, 0f)
        assertEquals(listOf(InputEvent.PointerMove(8f, 4f)), move(1, 14f, 2f))
    }

    @Test
    fun twoFingerTapIsRightClick() {
        down(1, 100f, 100f)
        down(2, 140f, 100f)
        wait(100)
        assertTrue(up(1).isEmpty())
        assertEquals(click(Button.RIGHT), up(2))
    }

    @Test
    fun threeFingerTapIsMiddleClick() {
        down(1, 100f, 100f)
        down(2, 140f, 100f)
        down(3, 180f, 100f)
        wait(100)
        up(1)
        up(2)
        assertEquals(click(Button.MIDDLE), up(3))
    }

    @Test
    fun doubleTapIsTwoLeftClicks() {
        down(1, 100f, 100f)
        wait(60)
        assertEquals(click(Button.LEFT), up(1))
        wait(120)
        down(2, 105f, 102f)
        wait(60)
        // ホストは続けて届いた 2 回のクリックをダブルクリックとして扱う
        assertEquals(click(Button.LEFT), up(2))
    }

    @Test
    fun tapThenHoldAndMoveIsDrag() {
        down(1, 100f, 100f)
        wait(60)
        up(1)
        wait(120)
        down(2, 102f, 100f)
        // 動き始めたら左ボタンを押し、そのまま移動する
        assertEquals(
            listOf(InputEvent.MouseButton(Button.LEFT, pressed = true), InputEvent.PointerMove(10f, 0f)),
            move(2, 112f, 100f),
        )
        assertEquals(listOf(InputEvent.PointerMove(20f, 0f)), move(2, 132f, 100f))
        wait(800)
        // 離したらボタンを離す
        assertEquals(listOf(InputEvent.MouseButton(Button.LEFT, pressed = false)), up(2))
    }

    @Test
    fun touchAfterDoubleTapTimeoutIsNotDrag() {
        down(1, 100f, 100f)
        wait(60)
        up(1)
        wait(400)
        down(2, 100f, 100f)
        assertEquals(listOf(InputEvent.PointerMove(10f, 0f)), move(2, 110f, 100f))
    }

    @Test
    fun touchFarFromPreviousTapIsNotDrag() {
        down(1, 100f, 100f)
        wait(60)
        up(1)
        wait(100)
        down(2, 300f, 300f)
        assertEquals(listOf(InputEvent.PointerMove(10f, 0f)), move(2, 310f, 300f))
    }

    @Test
    fun twoFingerMoveScrolls() {
        down(1, 100f, 100f)
        down(2, 140f, 100f)
        // 1 本目が下へ 32dp: 2 本の平均で 16dp = 1 段。従来の向きでは指を下へ → 下へスクロール(負)
        assertEquals(listOf(InputEvent.Scroll(vertical = -1f, horizontal = 0f)), move(1, 100f, 132f))
        assertEquals(listOf(InputEvent.Scroll(vertical = -1f, horizontal = 0f)), move(2, 140f, 132f))
        wait(100)
        up(1)
        // スクロールの後はクリックにしない
        assertTrue(up(2).isEmpty())
    }

    @Test
    fun naturalScrollIsReversed() {
        interpreter.settings = interpreter.settings.copy(scrollDirection = ScrollDirection.NATURAL)
        down(1, 100f, 100f)
        down(2, 140f, 100f)
        // 指を下へ → 内容が下へ動く → 上へスクロール(正)
        assertEquals(listOf(InputEvent.Scroll(vertical = 1f, horizontal = 0f)), move(1, 100f, 132f))
        // 指を右へ → 内容が右へ動く → 左へスクロール(負)
        assertEquals(listOf(InputEvent.Scroll(vertical = 0f, horizontal = -1f)), move(2, 172f, 100f))
    }

    @Test
    fun remainingFingerAfterScrollDoesNotJumpPointer() {
        down(1, 100f, 100f)
        down(2, 140f, 100f)
        move(1, 100f, 132f)
        up(2)
        // 2 本指の操作の後に残った 1 本の動きは無視する
        assertTrue(move(1, 150f, 132f).isEmpty())
    }

    @Test
    fun secondFingerAfterMovingStartsScroll() {
        down(1, 100f, 100f)
        move(1, 120f, 100f)
        down(2, 160f, 100f)
        assertEquals(listOf(InputEvent.Scroll(vertical = -1f, horizontal = 0f)), move(2, 160f, 132f))
    }

    @Test
    fun cancelReleasesDrag() {
        down(1, 100f, 100f)
        wait(60)
        up(1)
        wait(100)
        down(2, 100f, 100f)
        move(2, 120f, 100f)
        assertEquals(
            listOf(InputEvent.MouseButton(Button.LEFT, pressed = false)),
            interpreter.onTouch(TouchEvent.Cancel(now)),
        )
        // 打ち切った後は、次のタップもふつうのクリック
        down(3, 100f, 100f)
        wait(60)
        assertEquals(click(Button.LEFT), up(3))
    }

    @Test
    fun unknownPointerIsIgnored() {
        assertTrue(move(9, 10f, 10f).isEmpty())
        assertTrue(up(9).isEmpty())
    }
}
