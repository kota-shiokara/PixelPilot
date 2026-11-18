package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import jp.ikanoshiokara.pixelpilot.domain.hid.ReportId
import jp.ikanoshiokara.pixelpilot.model.input.Button
import jp.ikanoshiokara.pixelpilot.model.input.ConsumerUsage
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputBatcherTest {
    private val batcher = InputBatcher()

    @Test
    fun motionIsHeldUntilFlush() {
        assertTrue(batcher.add(InputEvent.PointerMove(3f, 1f)).isEmpty())
        assertTrue(batcher.add(InputEvent.PointerMove(2f, 1f)).isEmpty())
        assertTrue(batcher.hasPendingMotion)

        // 2 回の移動が 1 つのレポートにまとまる
        val data = batcher.flush().single().data
        assertEquals(5, data[1].toInt())
        assertEquals(2, data[3].toInt())
        assertFalse(batcher.hasPendingMotion)
        assertTrue(batcher.flush().isEmpty())
    }

    @Test
    fun moveAndScrollAreCombined() {
        batcher.add(InputEvent.PointerMove(1f, 0f))
        batcher.add(InputEvent.Scroll(vertical = 2f, horizontal = 0f))
        val data = batcher.flush().single().data
        assertEquals(1, data[1].toInt())
        assertEquals(2, data[5].toInt())
    }

    @Test
    fun buttonFlushesPendingMotionFirst() {
        batcher.add(InputEvent.PointerMove(4f, 0f))
        val reports = batcher.add(InputEvent.MouseButton(Button.LEFT, pressed = true))

        // 移動 → クリックの順に届く
        assertEquals(2, reports.size)
        assertEquals(4, reports[0].data[1].toInt())
        assertEquals(0, reports[0].data[0].toInt())
        assertEquals(1, reports[1].data[0].toInt())
        assertFalse(batcher.hasPendingMotion)
    }

    @Test
    fun keyIsSentImmediately() {
        val reports = batcher.add(InputEvent.Key(KeyboardUsage.A, pressed = true))
        assertEquals(listOf(ReportId.KEYBOARD), reports.map { it.reportId })
    }

    @Test
    fun consumerStillFlushesMotion() {
        // Consumer はまだ送らない(#52)が、溜めた移動は吐き出して順序を保つ
        batcher.add(InputEvent.PointerMove(1f, 0f))
        val reports = batcher.add(InputEvent.Consumer(ConsumerUsage.MUTE, pressed = true))
        assertEquals(listOf(ReportId.MOUSE), reports.map { it.reportId })
    }

    @Test
    fun resetDropsPendingMotionAndReleasesPressed() {
        batcher.add(InputEvent.MouseButton(Button.LEFT, pressed = true))
        batcher.add(InputEvent.PointerMove(10f, 0f))
        val released = batcher.reset()
        assertEquals(0, released.single().data[0].toInt())
        assertFalse(batcher.hasPendingMotion)
        assertTrue(batcher.flush().isEmpty())
    }
}
