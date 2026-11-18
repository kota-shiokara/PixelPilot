package jp.ikanoshiokara.pixelpilot.domain.hid

/**
 * 送信する 1 つの HID レポート。
 *
 * @property reportId [ReportId] のいずれか。`BluetoothHidDevice.sendReport` の `id` に渡す。
 * @property data Report ID を含まないレポート本体。
 */
class HidReport(val reportId: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is HidReport && reportId == other.reportId && data.contentEquals(other.data)

    override fun hashCode(): Int = 31 * reportId + data.contentHashCode()

    override fun toString(): String =
        "HidReport(id=$reportId, data=${data.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }})"
}
