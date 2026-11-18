package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class HidConnectionSignalTest {
    private val host = HostDevice(address = "00:11:22:33:44:55", name = "host")

    private fun ConnectionState.after(vararg signals: HidConnectionSignal): ConnectionState =
        signals.fold(this) { state, signal -> reduce(state, signal) }

    @Test
    fun normalFlowReachesConnected() {
        val state = ConnectionState.Registering.after(
            HidConnectionSignal.RegisterStarted,
            HidConnectionSignal.Registered,
            HidConnectionSignal.HostConnecting(host),
            HidConnectionSignal.HostConnected(host),
        )
        assertEquals(ConnectionState.Connected(host), state)
    }

    @Test
    fun registeredGoesToReady() {
        assertEquals(ConnectionState.Ready, ConnectionState.Registering.after(HidConnectionSignal.Registered))
    }

    @Test
    fun missingPermissionShowsPermissionRequired() {
        assertEquals(
            ConnectionState.PermissionRequired,
            ConnectionState.Registering.after(HidConnectionSignal.PermissionMissing),
        )
    }

    @Test
    fun unavailableProfileIsUnsupported() {
        assertEquals(
            ConnectionState.Unsupported,
            ConnectionState.Registering.after(HidConnectionSignal.RegisterStarted, HidConnectionSignal.ProfileUnavailable),
        )
    }

    @Test
    fun bluetoothOffBeforeConnectingIsBluetoothOff() {
        assertEquals(ConnectionState.BluetoothOff, ConnectionState.Ready.after(HidConnectionSignal.BluetoothTurnedOff))
        assertEquals(ConnectionState.BluetoothOff, ConnectionState.Registering.after(HidConnectionSignal.BluetoothTurnedOff))
    }

    @Test
    fun bluetoothOffWhileConnectedIsError() {
        // 使用中の画面に「Bluetooth がオフになりました」と出す(設計書 7.5)
        assertEquals(
            ConnectionState.Error(ErrorReason.BLUETOOTH_OFF),
            ConnectionState.Connected(host).after(HidConnectionSignal.BluetoothTurnedOff),
        )
        assertEquals(
            ConnectionState.Error(ErrorReason.BLUETOOTH_OFF),
            ConnectionState.Connecting(host).after(HidConnectionSignal.BluetoothTurnedOff),
        )
    }

    @Test
    fun disconnectWhileConnectedIsConnectionLost() {
        assertEquals(
            ConnectionState.Error(ErrorReason.CONNECTION_LOST),
            ConnectionState.Connected(host).after(HidConnectionSignal.HostDisconnected(host)),
        )
    }

    @Test
    fun disconnectWhileConnectingIsHostNotFound() {
        assertEquals(
            ConnectionState.Error(ErrorReason.HOST_NOT_FOUND),
            ConnectionState.Connecting(host).after(HidConnectionSignal.HostDisconnected(host)),
        )
    }

    @Test
    fun disconnectWhenNotConnectedKeepsState() {
        assertEquals(ConnectionState.Ready, ConnectionState.Ready.after(HidConnectionSignal.HostDisconnected(host)))
    }

    @Test
    fun registerFailureIsErrorUnlessDeviceStateIsMoreImportant() {
        assertEquals(
            ConnectionState.Error(ErrorReason.REGISTER_FAILED),
            ConnectionState.Registering.after(HidConnectionSignal.RegisterFailed),
        )
        assertEquals(ConnectionState.Unsupported, ConnectionState.Unsupported.after(HidConnectionSignal.RegisterFailed))
        assertEquals(ConnectionState.BluetoothOff, ConnectionState.BluetoothOff.after(HidConnectionSignal.RegisterFailed))
    }

    @Test
    fun registeredNotificationDoesNotOverrideConnection() {
        assertEquals(ConnectionState.Connected(host), ConnectionState.Connected(host).after(HidConnectionSignal.Registered))
    }

    @Test
    fun recoveringFromBluetoothOffRegistersAgain() {
        val state = ConnectionState.Connected(host).after(
            HidConnectionSignal.BluetoothTurnedOff,
            HidConnectionSignal.RegisterStarted,
            HidConnectionSignal.Registered,
        )
        assertEquals(ConnectionState.Ready, state)
    }

    @Test
    fun stopReturnsToRegistering() {
        assertEquals(ConnectionState.Registering, ConnectionState.Connected(host).after(HidConnectionSignal.Stopped))
    }
}
