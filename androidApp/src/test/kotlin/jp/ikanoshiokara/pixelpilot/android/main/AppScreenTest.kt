package jp.ikanoshiokara.pixelpilot.android.main

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreenTest {
    private val host = HostDevice(address = "AA:AA:AA:AA:AA:AA", name = "desk")

    @Test
    fun connectedGoesToMain() {
        assertEquals(AppScreen.MAIN, nextScreen(AppScreen.HOST_SELECT, ConnectionState.Connected(host)))
    }

    @Test
    fun hostSelectStaysUntilConnected() {
        listOf(
            ConnectionState.Registering,
            ConnectionState.Ready,
            ConnectionState.Connecting(host),
            ConnectionState.Error(ErrorReason.HOST_NOT_FOUND),
        ).forEach { state ->
            assertEquals(state.toString(), AppScreen.HOST_SELECT, nextScreen(AppScreen.HOST_SELECT, state))
        }
    }

    @Test
    fun mainStaysWhileReconnectingOrOnErrors() {
        listOf(
            ConnectionState.Error(ErrorReason.CONNECTION_LOST),
            ConnectionState.Error(ErrorReason.BLUETOOTH_OFF),
            ConnectionState.Error(ErrorReason.HOST_NOT_FOUND),
            // フォアグラウンドに戻ったときの再登録 → 自動接続
            ConnectionState.Registering,
            ConnectionState.Ready,
            ConnectionState.Connecting(host),
        ).forEach { state ->
            assertEquals(state.toString(), AppScreen.MAIN, nextScreen(AppScreen.MAIN, state))
        }
    }

    @Test
    fun deviceStatesNeedingGuidanceGoBackToHostSelect() {
        listOf(
            ConnectionState.PermissionRequired,
            ConnectionState.BluetoothOff,
            ConnectionState.Unsupported,
        ).forEach { state ->
            assertEquals(state.toString(), AppScreen.HOST_SELECT, nextScreen(AppScreen.MAIN, state))
        }
    }

    @Test
    fun resetToLeavesOnlyTheGivenScreen() {
        val backStack = mutableListOf(AppScreen.HOST_SELECT)
        backStack.resetTo(AppScreen.MAIN)
        assertEquals(listOf(AppScreen.MAIN), backStack)
        backStack.resetTo(AppScreen.HOST_SELECT)
        assertEquals(listOf(AppScreen.HOST_SELECT), backStack)
    }

    @Test
    fun resetToSameScreenKeepsBackStack() {
        val backStack = mutableListOf(AppScreen.MAIN)
        backStack.resetTo(AppScreen.MAIN)
        assertEquals(listOf(AppScreen.MAIN), backStack)
    }
}
