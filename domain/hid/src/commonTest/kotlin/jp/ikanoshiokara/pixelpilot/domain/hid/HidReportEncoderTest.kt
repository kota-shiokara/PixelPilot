package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.Button
import jp.ikanoshiokara.pixelpilot.model.input.ConsumerUsage
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HidReportEncoderTest {
    private val encoder = HidReportEncoder()

    @Test
    fun routesEventsToReportIds() {
        assertEquals(ReportId.MOUSE, encoder.encode(InputEvent.PointerMove(3f, 0f)).single().reportId)
        assertEquals(ReportId.MOUSE, encoder.encode(InputEvent.Scroll(1f, 0f)).single().reportId)
        assertEquals(ReportId.MOUSE, encoder.encode(InputEvent.MouseButton(Button.LEFT, true)).single().reportId)
        assertEquals(ReportId.KEYBOARD, encoder.encode(InputEvent.Key(KeyboardUsage.A, true)).single().reportId)
    }

    @Test
    fun consumerIsNotEncodedYet() {
        // Consumer Control のレポートは v2.x(#52)で実装する
        assertTrue(encoder.encode(InputEvent.Consumer(ConsumerUsage.PLAY_PAUSE, true)).isEmpty())
    }

    @Test
    fun scrollGoesToWheelAndPan() {
        val data = encoder.encode(InputEvent.Scroll(vertical = 2f, horizontal = -1f)).single().data
        assertEquals(2, data[5].toInt())
        assertEquals(-1, data[6].toInt())
    }

    @Test
    fun resetReleasesKeyboardThenMouse() {
        encoder.encode(InputEvent.Key(KeyboardUsage.A, true))
        encoder.encode(InputEvent.MouseButton(Button.LEFT, true))
        assertEquals(listOf(ReportId.KEYBOARD, ReportId.MOUSE), encoder.reset().map { it.reportId })
        assertTrue(encoder.reset().isEmpty())
    }
}
