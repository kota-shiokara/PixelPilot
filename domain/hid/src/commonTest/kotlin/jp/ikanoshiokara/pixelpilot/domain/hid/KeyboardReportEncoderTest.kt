package jp.ikanoshiokara.pixelpilot.domain.hid

import jp.ikanoshiokara.pixelpilot.model.input.KeyboardUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KeyboardReportEncoderTest {
    private val encoder = KeyboardReportEncoder()

    @Test
    fun reportHasKeyboardIdAndEightBytes() {
        val report = encoder.key(KeyboardUsage.A, pressed = true).single()
        assertEquals(ReportId.KEYBOARD, report.reportId)
        assertEquals(ReportLength.KEYBOARD_INPUT, report.data.size)
    }

    @Test
    fun keyPressAndReleaseAreSeparateReports() {
        assertEquals(keyboard(keys = listOf(KeyboardUsage.A)), encoder.key(KeyboardUsage.A, true).single())
        assertEquals(keyboard(), encoder.key(KeyboardUsage.A, false).single())
    }

    @Test
    fun modifiersGoToFirstByteNotKeySlots() {
        // Ctrl + C
        assertEquals(keyboard(modifiers = 0x01), encoder.key(KeyboardUsage.LEFT_CONTROL, true).single())
        assertEquals(
            keyboard(modifiers = 0x01, keys = listOf(KeyboardUsage.C)),
            encoder.key(KeyboardUsage.C, true).single(),
        )
        assertEquals(keyboard(modifiers = 0x01), encoder.key(KeyboardUsage.C, false).single())
        assertEquals(keyboard(), encoder.key(KeyboardUsage.LEFT_CONTROL, false).single())
    }

    @Test
    fun multipleModifiersCombineBits() {
        encoder.key(KeyboardUsage.LEFT_SHIFT, true)
        assertEquals(keyboard(modifiers = 0x02 or 0x80), encoder.key(KeyboardUsage.RIGHT_GUI, true).single())
    }

    @Test
    fun keysAreKeptInPressOrderAndShiftOnRelease() {
        encoder.key(KeyboardUsage.A, true)
        encoder.key(KeyboardUsage.B, true)
        assertEquals(
            keyboard(keys = listOf(KeyboardUsage.A, KeyboardUsage.B, KeyboardUsage.C)),
            encoder.key(KeyboardUsage.C, true).single(),
        )
        assertEquals(
            keyboard(keys = listOf(KeyboardUsage.A, KeyboardUsage.C)),
            encoder.key(KeyboardUsage.B, false).single(),
        )
    }

    @Test
    fun sixKeysFitAndSeventhCausesRollOver() {
        val keys = listOf(KeyboardUsage.A, KeyboardUsage.B, KeyboardUsage.C, KeyboardUsage.D, KeyboardUsage.E, KeyboardUsage.F)
        val reports = keys.map { encoder.key(it, true).single() }
        assertEquals(keyboard(keys = keys), reports.last())

        encoder.key(KeyboardUsage.LEFT_SHIFT, true)
        // 7 個目: キー欄すべてを ErrorRollOver にする。修飾キーはそのまま送る
        assertEquals(
            keyboard(modifiers = 0x02, keys = List(6) { KeyboardUsage.ERROR_ROLL_OVER }),
            encoder.key(KeyboardUsage.G, true).single(),
        )
        // 6 個に戻ったら通常のレポートに戻る
        assertEquals(
            keyboard(modifiers = 0x02, keys = keys.drop(1) + KeyboardUsage.G),
            encoder.key(KeyboardUsage.A, false).single(),
        )
    }

    @Test
    fun repeatedPressOrReleaseSendsNothing() {
        encoder.key(KeyboardUsage.A, true)
        assertTrue(encoder.key(KeyboardUsage.A, true).isEmpty())
        assertTrue(encoder.key(KeyboardUsage.B, false).isEmpty())
        encoder.key(KeyboardUsage.LEFT_ALT, true)
        assertTrue(encoder.key(KeyboardUsage.LEFT_ALT, true).isEmpty())
    }

    @Test
    fun jisKeysAboveSevenBitsAreSentAsUnsignedBytes() {
        val report = encoder.key(KeyboardUsage.INTERNATIONAL_3, true).single()
        assertEquals(0x89, report.data[2].toInt() and 0xFF)
    }

    @Test
    fun rejectsOutOfRangeUsage() {
        assertFailsWith<IllegalArgumentException> { encoder.key(0x00, true) }
        assertFailsWith<IllegalArgumentException> { encoder.key(0x100, true) }
    }

    @Test
    fun resetReleasesEverything() {
        encoder.key(KeyboardUsage.LEFT_CONTROL, true)
        encoder.key(KeyboardUsage.A, true)
        assertEquals(keyboard(), encoder.reset().single())
        assertTrue(encoder.reset().isEmpty())
    }

    private fun keyboard(modifiers: Int = 0, keys: List<Int> = emptyList()): HidReport {
        require(keys.size <= 6)
        val data = ByteArray(8)
        data[0] = modifiers.toByte()
        keys.forEachIndexed { i, usage -> data[2 + i] = usage.toByte() }
        return HidReport(ReportId.KEYBOARD, data)
    }
}
