package jp.ikanoshiokara.pixelpilot.domain.hid

import kotlin.test.Test
import kotlin.test.assertEquals

class HidReportDescriptorTest {
    /**
     * ゴールデンテスト。docs/design/v2.0-standard.md 2.1〜2.3 のバイト列と完全に一致すること。
     * 意図して変更する場合は、設計書とこの期待値を両方更新する。
     */
    @Test
    fun matchesDesignDocument() {
        val expected = listOf(
            // 2.1 キーボード
            "05 01 09 06 A1 01 85 01",
            "05 07 19 E0 29 E7 15 00 25 01 75 01 95 08 81 02",
            "95 01 75 08 81 01",
            "95 05 75 01 05 08 19 01 29 05 91 02",
            "95 01 75 03 91 01",
            "95 06 75 08 15 00 26 FF 00 05 07 19 00 29 FF 81 00",
            "C0",
            // 2.2 マウス
            "05 01 09 02 A1 01 85 02 09 01 A1 00",
            "05 09 19 01 29 05 15 00 25 01 95 05 75 01 81 02",
            "95 01 75 03 81 01",
            "05 01 09 30 09 31 16 01 80 26 FF 7F 75 10 95 02 81 06",
            "09 38 15 81 25 7F 75 08 95 01 81 06",
            "05 0C 0A 38 02 15 81 25 7F 75 08 95 01 81 06",
            "C0 C0",
            // 2.3 Consumer Control
            "05 0C 09 01 A1 01 85 03",
            "15 00 26 FF 03 19 00 2A FF 03 75 10 95 01 81 00",
            "C0",
        ).joinToString(" ")

        assertEquals(expected, HidReportDescriptor.bytes.toHex())
    }

    /** Report Descriptor を読んで計算したレポート長が、エンコーダが生成する長さと一致すること。 */
    @Test
    fun reportLengthsMatchDescriptor() {
        val bits = ReportBits.parse(HidReportDescriptor.bytes)

        assertEquals(ReportLength.KEYBOARD_INPUT * 8, bits.input.getValue(ReportId.KEYBOARD))
        assertEquals(ReportLength.KEYBOARD_OUTPUT * 8, bits.output.getValue(ReportId.KEYBOARD))
        assertEquals(ReportLength.MOUSE_INPUT * 8, bits.input.getValue(ReportId.MOUSE))
        assertEquals(ReportLength.CONSUMER_INPUT * 8, bits.input.getValue(ReportId.CONSUMER))
        assertEquals(setOf(ReportId.KEYBOARD), bits.output.keys)
    }

    @Test
    fun bytesReturnsDefensiveCopy() {
        val first = HidReportDescriptor.bytes
        first[0] = 0
        assertEquals(0x05, HidReportDescriptor.bytes[0].toInt())
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { (it.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0') }

    /**
     * Report Descriptor の short item を読み、Report ID ごとの Input / Output のビット数を数える(テスト用の最小実装)。
     * 仕様: Device Class Definition for HID 1.11, 6.2.2 Report Descriptor。
     */
    private class ReportBits(val input: Map<Int, Int>, val output: Map<Int, Int>) {
        companion object {
            private const val TYPE_MAIN = 0
            private const val TYPE_GLOBAL = 1
            private const val TAG_INPUT = 0x8
            private const val TAG_OUTPUT = 0x9
            private const val TAG_REPORT_SIZE = 0x7
            private const val TAG_REPORT_ID = 0x8
            private const val TAG_REPORT_COUNT = 0x9

            fun parse(descriptor: ByteArray): ReportBits {
                val input = mutableMapOf<Int, Int>()
                val output = mutableMapOf<Int, Int>()
                var reportId = 0
                var reportSize = 0
                var reportCount = 0
                var i = 0
                while (i < descriptor.size) {
                    val prefix = descriptor[i].toInt() and 0xFF
                    val size = when (prefix and 0x03) {
                        3 -> 4
                        else -> prefix and 0x03
                    }
                    val type = (prefix shr 2) and 0x03
                    val tag = prefix shr 4
                    var value = 0
                    for (n in 0 until size) {
                        value = value or ((descriptor[i + 1 + n].toInt() and 0xFF) shl (8 * n))
                    }
                    when {
                        type == TYPE_GLOBAL && tag == TAG_REPORT_SIZE -> reportSize = value
                        type == TYPE_GLOBAL && tag == TAG_REPORT_COUNT -> reportCount = value
                        type == TYPE_GLOBAL && tag == TAG_REPORT_ID -> reportId = value
                        type == TYPE_MAIN && tag == TAG_INPUT ->
                            input[reportId] = (input[reportId] ?: 0) + reportSize * reportCount
                        type == TYPE_MAIN && tag == TAG_OUTPUT ->
                            output[reportId] = (output[reportId] ?: 0) + reportSize * reportCount
                    }
                    i += 1 + size
                }
                return ReportBits(input, output)
            }
        }
    }
}
