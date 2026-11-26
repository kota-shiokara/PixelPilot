package jp.ikanoshiokara.pixelpilot.android.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.ikanoshiokara.pixelpilot.android.R
import jp.ikanoshiokara.pixelpilot.android.settings.SettingsScreen
import jp.ikanoshiokara.pixelpilot.android.settings.SettingsViewModel
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors
import jp.ikanoshiokara.pixelpilot.view.mouse.MouseScreen
import org.koin.androidx.compose.koinViewModel

/** メイン画面のタブ。設定はタブ列の右端の入口(画面デザインの仮案)。 */
enum class MainTab(val label: Int) {
    MOUSE(R.string.tab_mouse),
    KEYBOARD(R.string.tab_keyboard),
    SETTINGS(R.string.tab_settings),
}

/**
 * メイン画面(docs/design/v2.0-standard.md 9.)。上に帯は置かず、下部のタブ列でマウス / キーボード / 設定を切り替える。
 *
 * - 開いている間は画面を点けたままにする(7.3)。設定でオフにできる(既定: オン)
 * - システムの「戻る」でアプリを終了しない(誤操作で接続が切れないように)。マウス以外のタブではマウスに戻る
 * - マウスの画面には、保存した設定(移動感度・スクロール方向・クリックボタンの表示)を渡す
 *
 * @param onDisconnect 接続を切って、接続先を選び直す
 */
@Composable
fun MainScreen(onDisconnect: () -> Unit, settingsViewModel: SettingsViewModel = koinViewModel()) {
    var tab by rememberSaveable { mutableStateOf(MainTab.MOUSE) }
    val mouseSettings by settingsViewModel.mouseSettings.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()

    BackHandler {
        if (tab != MainTab.MOUSE) tab = MainTab.MOUSE
    }
    if (appSettings.keepScreenOn) KeepScreenOn()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                MainTab.MOUSE -> MouseScreen(settings = mouseSettings)
                MainTab.KEYBOARD -> Placeholder(stringResource(R.string.keyboard_coming))
                MainTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel, onDisconnect = onDisconnect)
            }
        }
        TabBar(selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            TextButton(
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .weight(if (tab == MainTab.SETTINGS) 0.6f else 1f)
                    .height(48.dp)
                    .background(
                        if (isSelected) PixelPilotColors.Surface else PixelPilotColors.Background,
                        RoundedCornerShape(14.dp),
                    )
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
            ) {
                Text(
                    text = stringResource(tab.label),
                    color = if (isSelected) PixelPilotColors.Accent else PixelPilotColors.OnSurfaceMuted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = PixelPilotColors.OnSurfaceMuted)
    }
}

/** この部品が出ている間、画面を点けたままにする(FLAG_KEEP_SCREEN_ON と同じ)。外れると元に戻す。 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
