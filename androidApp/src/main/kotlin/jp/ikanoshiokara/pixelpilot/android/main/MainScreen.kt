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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors
import jp.ikanoshiokara.pixelpilot.view.mouse.MouseScreen

/** メイン画面のタブ。設定はタブ列の右端の入口(画面デザインの仮案)。 */
enum class MainTab(val label: Int) {
    MOUSE(R.string.tab_mouse),
    KEYBOARD(R.string.tab_keyboard),
    SETTINGS(R.string.tab_settings),
}

/**
 * メイン画面(docs/design/v2.0-standard.md 9.)。上に帯は置かず、下部のタブ列でマウス / キーボード / 設定を切り替える。
 *
 * - 開いている間は画面を点けたままにする(7.3。設定で切り替えるのは #46)
 * - システムの「戻る」でアプリを終了しない(誤操作で接続が切れないように)。マウス以外のタブではマウスに戻る
 *
 * @param onDisconnect 接続を切って、接続先を選び直す
 */
@Composable
fun MainScreen(onDisconnect: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.MOUSE) }

    BackHandler {
        if (tab != MainTab.MOUSE) tab = MainTab.MOUSE
    }
    KeepScreenOn()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                // 設定の保存(#46)までは既定値で動かす
                MainTab.MOUSE -> MouseScreen(settings = MouseSettings())
                MainTab.KEYBOARD -> Placeholder(stringResource(R.string.keyboard_coming))
                MainTab.SETTINGS -> SettingsPlaceholder(onDisconnect)
            }
        }
        TabBar(selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
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

/** 設定画面(#46)ができるまでの仮の表示。接続先の選び直しだけ置く。 */
@Composable
private fun SettingsPlaceholder(onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_coming),
            style = MaterialTheme.typography.bodyLarge,
            color = PixelPilotColors.OnSurfaceMuted,
        )
        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(text = stringResource(R.string.disconnect), color = PixelPilotColors.OnSurface)
        }
    }
}

/** この画面が出ている間、画面を点けたままにする(FLAG_KEEP_SCREEN_ON と同じ)。 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
