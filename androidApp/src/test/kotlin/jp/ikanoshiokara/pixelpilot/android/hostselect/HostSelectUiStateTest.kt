package jp.ikanoshiokara.pixelpilot.android.hostselect

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class HostSelectUiStateTest {
    private val desk = HostDevice(address = "AA:AA:AA:AA:AA:AA", name = "desk")
    private val laptop = HostDevice(address = "BB:BB:BB:BB:BB:BB", name = "laptop")

    private fun state(
        connection: ConnectionState,
        permissionBlocked: Boolean = false,
        lastHostAddress: String? = null,
        pairedHosts: List<HostDevice> = emptyList(),
        pairing: PairingState = PairingState.Idle,
    ) = hostSelectUiState(connection, permissionBlocked, lastHostAddress, pairedHosts, pairing)

    @Test
    fun registeringShowsChecking() {
        assertEquals(HostSelectUiState.Checking, state(ConnectionState.Registering))
    }

    @Test
    fun permissionStateDependsOnWhetherDialogCanStillBeShown() {
        assertEquals(HostSelectUiState.NeedsPermission(blocked = false), state(ConnectionState.PermissionRequired))
        assertEquals(
            HostSelectUiState.NeedsPermission(blocked = true),
            state(ConnectionState.PermissionRequired, permissionBlocked = true),
        )
    }

    @Test
    fun bluetoothOffAndUnsupported() {
        assertEquals(HostSelectUiState.BluetoothOff, state(ConnectionState.BluetoothOff))
        assertEquals(HostSelectUiState.Unsupported, state(ConnectionState.Unsupported))
        // 使用中にオフになったエラーも、ホスト選択画面では案内として出す
        assertEquals(HostSelectUiState.BluetoothOff, state(ConnectionState.Error(ErrorReason.BLUETOOTH_OFF)))
    }

    @Test
    fun readySplitsLastHostFromOthers() {
        val result = state(ConnectionState.Ready, lastHostAddress = laptop.address, pairedHosts = listOf(desk, laptop))
        assertEquals(
            HostSelectUiState.Hosts(
                lastHost = laptop,
                otherHosts = listOf(desk),
                connecting = null,
                error = null,
                pairing = PairingState.Idle,
            ),
            result,
        )
    }

    @Test
    fun lastHostThatIsNoLongerPairedIsNotShown() {
        val result = state(ConnectionState.Ready, lastHostAddress = "CC:CC:CC:CC:CC:CC", pairedHosts = listOf(desk))
        assertEquals(null, (result as HostSelectUiState.Hosts).lastHost)
        assertEquals(listOf(desk), result.otherHosts)
    }

    @Test
    fun connectingAndErrorsAreShownOnTheHostList() {
        val connecting = state(ConnectionState.Connecting(desk), pairedHosts = listOf(desk)) as HostSelectUiState.Hosts
        assertEquals(desk, connecting.connecting)

        val notFound = state(ConnectionState.Error(ErrorReason.HOST_NOT_FOUND), pairedHosts = listOf(desk)) as HostSelectUiState.Hosts
        assertEquals(ErrorReason.HOST_NOT_FOUND, notFound.error)
        assertEquals(null, notFound.connecting)
    }

    @Test
    fun pairingStateIsPassedThrough() {
        val pairing = PairingState.Discoverable(remainingSeconds = 102, totalSeconds = 120)
        val result = state(ConnectionState.Ready, pairing = pairing) as HostSelectUiState.Hosts
        assertEquals(pairing, result.pairing)
    }

    @Test
    fun connectedGoesToMainScreen() {
        assertEquals(HostSelectUiState.Connected(desk), state(ConnectionState.Connected(desk)))
    }
}
