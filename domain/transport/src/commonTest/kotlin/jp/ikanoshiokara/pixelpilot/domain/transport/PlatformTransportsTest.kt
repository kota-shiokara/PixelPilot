package jp.ikanoshiokara.pixelpilot.domain.transport

import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTransportsTest {
    @Test
    fun desktopIsNotRequiredWhenBluetoothHidIsAvailable() {
        assertFalse(listOf(TransportKind.BLUETOOTH_HID).requiresDesktop)
        assertFalse(listOf(TransportKind.BLUETOOTH_HID, TransportKind.AGENT_USB).requiresDesktop)
    }

    @Test
    fun desktopIsRequiredWithoutBluetoothHid() {
        // iOS や、HID Device プロファイルが無効化された Android 端末
        assertTrue(listOf(TransportKind.AGENT_LAN, TransportKind.AGENT_USB).requiresDesktop)
        assertTrue(emptyList<TransportKind>().requiresDesktop)
    }
}
