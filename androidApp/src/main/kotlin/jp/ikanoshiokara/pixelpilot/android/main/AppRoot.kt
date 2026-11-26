package jp.ikanoshiokara.pixelpilot.android.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import jp.ikanoshiokara.pixelpilot.android.hostselect.HostSelectRoute
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import org.koin.compose.koinInject

/**
 * アプリの画面の切り替え(ホスト選択 ⇔ メイン)。どの画面を出すかは [nextScreen] で決める。
 */
@Composable
fun AppRoot(controller: BluetoothHidController = koinInject()) {
    val state by controller.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOST_SELECT) }

    LaunchedEffect(state) {
        screen = nextScreen(screen, state)
    }

    when (screen) {
        AppScreen.HOST_SELECT -> HostSelectRoute()
        AppScreen.MAIN -> MainScreen(
            onDisconnect = {
                controller.disconnect()
                screen = AppScreen.HOST_SELECT
            },
        )
    }
}
