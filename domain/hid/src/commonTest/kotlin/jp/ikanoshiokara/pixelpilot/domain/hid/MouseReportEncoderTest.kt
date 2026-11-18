package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.Button
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MouseReportEncoderTest {
    private val encoder = MouseReportEncoder()

    @Test
    fun reportHasMouseIdAndSevenBytes() {
        val report = encoder.motion(1f, 0f).single()
        assertEquals(ReportId.MOUSE, report.reportId)
        assertEquals(ReportLength.MOUSE_INPUT, report.data.size)
    }

    @Test
    fun xAndYAreLittleEndianSigned16Bit() {
        assertEquals(mouse(x = 0x1234, y = -2), encoder.motion(0x1234.toFloat(), -2f).single())
        assertEquals(bytes(0x00, 0x34, 0x12, 0xFE, 0xFF, 0x00, 0x00), encoder.motion(0x1234.toFloat(), -2f).single().data.toList())
    }

    @Test
    fun fractionsAreCarriedOver() {
        assertTrue(encoder.motion(0.4f, 0f).isEmpty())
        assertTrue(encoder.motion(0.4f, 0f).isEmpty())
        // 0.4 × 3 = 1.2 → 1 を送り、0.2 を持ち越す
        assertEquals(mouse(x = 1), encoder.motion(0.4f, 0f).single())
        // 0.2 + 0.8 = 1.0
        assertEquals(mouse(x = 1), encoder.motion(0.8f, 0f).single())
    }

    @Test
    fun negativeFractionsAreTruncatedTowardZero() {
        assertTrue(encoder.motion(0f, -0.6f).isEmpty())
        // -1.2 → -1 を送り、-0.2 を持ち越す
        assertEquals(mouse(y = -1), encoder.motion(0f, -0.6f).single())
        assertTrue(encoder.motion(0f, -0.7f).isEmpty())
    }

    @Test
    fun manySmallMovesAddUpWithoutLoss() {
        val sent = (1..1000).flatMap { encoder.motion(0.1f, 0f) }.sumOf { it.x() }
        assertEquals(100, sent)
    }

    @Test
    fun largeMovesAreSplitInto16BitReports() {
        val reports = encoder.motion(40000f, -70000f)
        assertEquals(
            listOf(
                mouse(x = 32767, y = -32767),
                mouse(x = 7233, y = -32767),
                mouse(x = 0, y = -4466),
            ),
            reports,
        )
    }

    @Test
    fun largeScrollIsSplitInto8BitReports() {
        val reports = encoder.motion(0f, 0f, wheel = 300f, pan = -130f)
        assertEquals(
            listOf(
                mouse(wheel = 127, pan = -127),
                mouse(wheel = 127, pan = -3),
                mouse(wheel = 46),
            ),
            reports,
        )
    }

    @Test
    fun wheelMultiplierScalesScroll() {
        val encoder = MouseReportEncoder(wheelMultiplier = 4)
        assertEquals(mouse(wheel = 2), encoder.motion(0f, 0f, wheel = 0.5f).single())
        assertFailsWith<IllegalArgumentException> { MouseReportEncoder(wheelMultiplier = 0) }
    }

    @Test
    fun buttonPressAndReleaseAreSeparateReports() {
        assertEquals(mouse(buttons = 0x01), encoder.button(Button.LEFT, pressed = true).single())
        assertEquals(mouse(buttons = 0x03), encoder.button(Button.RIGHT, pressed = true).single())
        assertEquals(mouse(buttons = 0x02), encoder.button(Button.LEFT, pressed = false).single())
        assertEquals(mouse(buttons = 0x00), encoder.button(Button.RIGHT, pressed = false).single())
    }

    @Test
    fun allFiveButtonsUseTheirBits() {
        assertEquals(mouse(buttons = 0x04), encoder.button(Button.MIDDLE, true).single())
        encoder.button(Button.MIDDLE, false)
        assertEquals(mouse(buttons = 0x08), encoder.button(Button.BACK, true).single())
        encoder.button(Button.BACK, false)
        assertEquals(mouse(buttons = 0x10), encoder.button(Button.FORWARD, true).single())
    }

    @Test
    fun repeatedPressSendsNothing() {
        encoder.button(Button.LEFT, pressed = true)
        assertTrue(encoder.button(Button.LEFT, pressed = true).isEmpty())
        assertTrue(encoder.button(Button.RIGHT, pressed = false).isEmpty())
    }

    @Test
    fun motionKeepsPressedButtonsForDrag() {
        encoder.button(Button.LEFT, pressed = true)
        assertEquals(mouse(buttons = 0x01, x = 5, y = 3), encoder.motion(5f, 3f).single())
    }

    @Test
    fun resetReleasesButtonsAndDropsFractions() {
        encoder.button(Button.LEFT, pressed = true)
        encoder.motion(0.9f, 0f)
        assertEquals(mouse(buttons = 0x00), encoder.reset().single())
        assertTrue(encoder.reset().isEmpty())
        // 0.9 の端数は捨てられている
        assertTrue(encoder.motion(0.5f, 0f).isEmpty())
    }

    private fun mouse(buttons: Int = 0, x: Int = 0, y: Int = 0, wheel: Int = 0, pan: Int = 0) = HidReport(
        ReportId.MOUSE,
        byteArrayOf(
            buttons.toByte(),
            (x and 0xFF).toByte(),
            ((x shr 8) and 0xFF).toByte(),
            (y and 0xFF).toByte(),
            ((y shr 8) and 0xFF).toByte(),
            wheel.toByte(),
            pan.toByte(),
        ),
    )

    private fun bytes(vararg values: Int): List<Byte> = values.map { it.toByte() }

    private fun HidReport.x(): Int = ((data[2].toInt() shl 8) or (data[1].toInt() and 0xFF)).toShort().toInt()
}
