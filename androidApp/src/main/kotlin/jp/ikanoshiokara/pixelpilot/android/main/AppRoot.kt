package jp.ikanoshiokara.pixelpilot.android.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import jp.ikanoshiokara.pixelpilot.android.hostselect.HostSelectRoute
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import org.koin.compose.koinInject

/**
 * アプリの画面遷移(ホスト選択 ⇔ メイン)。Navigation 3 の [NavDisplay] で、バックスタック(画面のリスト)を描画する。
 *
 * - どの画面を出すかは接続状態から [nextScreen] で決め、バックスタックを [resetTo] で置き換える
 * - ホスト選択 ⇔ メインは「戻る」で行き来する関係ではないため、バックスタックには常に 1 画面だけを置く。
 *   メインでの「戻る」は [MainScreen] が受け取り、アプリを終了しない(docs/design/v2.0-standard.md 13. 決定事項 8)
 */
@Composable
fun AppRoot(controller: BluetoothHidController = koinInject()) {
    val state by controller.state.collectAsState()
    val backStack = rememberSaveable(saver = backStackSaver) { mutableStateListOf(AppScreen.HOST_SELECT) }

    LaunchedEffect(state) {
        val current = backStack.last()
        backStack.resetTo(nextScreen(current, state))
    }

    NavDisplay(
        backStack = backStack,
        // バックスタックは常に 1 画面なので、NavDisplay が戻す先は無い。
        // ホスト選択での「戻る」はアプリの終了(Activity に任せる)、メインでは MainScreen が受け取る
        onBack = {},
        entryProvider = { screen ->
            NavEntry(screen) {
                when (screen) {
                    AppScreen.HOST_SELECT -> HostSelectRoute()
                    AppScreen.MAIN -> MainScreen(
                        onDisconnect = {
                            controller.disconnect()
                            backStack.resetTo(AppScreen.HOST_SELECT)
                        },
                    )
                }
            }
        },
    )
}

/** 画面の回転・プロセスの再生成でもバックスタックを保つ(AppScreen は enum のため、そのまま保存できる)。 */
private val backStackSaver = listSaver<SnapshotStateList<AppScreen>, AppScreen>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)
