package jp.ikanoshiokara.pixelpilot.android.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import jp.ikanoshiokara.pixelpilot.android.BuildConfig
import jp.ikanoshiokara.pixelpilot.android.R
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors
import kotlin.math.roundToInt

/** 移動感度のつまみの刻み(0.1 ずつ)。 */
private const val SENSITIVITY_STEP = 0.1f

/**
 * 設定画面(docs/design/v2.0-standard.md 9.)。メイン画面の「設定」タブに出す。
 *
 * v2.1 の前半は共通の設定(マウス・画面)と、接続先の選び直し・バージョンを置く。
 * ホストごとの設定(OS・キーボード配列)は #46 の後半で「接続先」の項目に足す。
 *
 * @param onDisconnect 接続を切って、接続先を選び直す
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onDisconnect: () -> Unit) {
    val mouse by viewModel.mouseSettings.collectAsState()
    val app by viewModel.appSettings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = PixelPilotColors.OnSurface,
            modifier = Modifier.semantics { heading() },
        )

        SectionHeader(stringResource(R.string.settings_section_mouse))
        SensitivitySetting(mouse.sensitivity, onChange = viewModel::setSensitivity)
        ScrollDirectionSetting(mouse.scrollDirection, onChange = viewModel::setScrollDirection)
        SwitchSetting(
            title = stringResource(R.string.settings_show_buttons),
            description = stringResource(R.string.settings_show_buttons_note),
            checked = mouse.showButtons,
            onChange = viewModel::setShowButtons,
        )

        SectionHeader(stringResource(R.string.settings_section_screen))
        SwitchSetting(
            title = stringResource(R.string.settings_keep_screen_on),
            description = stringResource(R.string.settings_keep_screen_on_note),
            checked = app.keepScreenOn,
            onChange = viewModel::setKeepScreenOn,
        )

        SectionHeader(stringResource(R.string.settings_section_connection))
        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(text = stringResource(R.string.disconnect), color = PixelPilotColors.OnSurface)
        }

        HorizontalDivider(color = PixelPilotColors.Outline, modifier = Modifier.padding(top = 8.dp))
        // デバッグ版は「2.0.0-debug+<コミット>」になり、どのコードのビルドか分かる(#99)
        Text(
            text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = PixelPilotColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = PixelPilotColors.Accent,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
    )
}

/**
 * 移動感度。つまみを動かしている間は画面の中だけで値を変え、離したときに保存する(動かすたびに書き込まないため)。
 */
@Composable
private fun SensitivitySetting(sensitivity: Float, onChange: (Float) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: sensitivity
    val range = MouseSettings.SENSITIVITY_RANGE
    val label = stringResource(R.string.settings_sensitivity_value, shown)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_sensitivity),
                style = MaterialTheme.typography.bodyLarge,
                color = PixelPilotColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = PixelPilotColors.OnSurfaceMuted)
        }
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let(onChange)
                dragging = null
            },
            valueRange = range,
            // 刻みの数(両端を除く)。0.5〜2.0 を 0.1 ずつなら 14
            steps = ((range.endInclusive - range.start) / SENSITIVITY_STEP).roundToInt() - 1,
            modifier = Modifier.semantics { stateDescription = label },
        )
    }
}

@Composable
private fun ScrollDirectionSetting(direction: ScrollDirection, onChange: (ScrollDirection) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.settings_scroll_direction),
            style = MaterialTheme.typography.bodyLarge,
            color = PixelPilotColors.OnSurface,
        )
        Column(modifier = Modifier.selectableGroup()) {
            ScrollDirection.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(selected = option == direction, role = Role.RadioButton, onClick = { onChange(option) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 行全体で選ぶため、ボタン自体のクリックは受けない
                    RadioButton(selected = option == direction, onClick = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = stringResource(option.label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelPilotColors.OnSurface,
                        )
                        Text(
                            text = stringResource(option.note),
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelPilotColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}

private val ScrollDirection.label: Int
    get() = when (this) {
        ScrollDirection.NATURAL -> R.string.settings_scroll_natural
        ScrollDirection.TRADITIONAL -> R.string.settings_scroll_traditional
    }

private val ScrollDirection.note: Int
    get() = when (this) {
        ScrollDirection.NATURAL -> R.string.settings_scroll_natural_note
        ScrollDirection.TRADITIONAL -> R.string.settings_scroll_traditional_note
    }

/** オン・オフの項目。行全体を押して切り替えられるようにし、補足の文言を下に添える。 */
@Composable
private fun SwitchSetting(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = PixelPilotColors.OnSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = PixelPilotColors.OnSurfaceMuted)
        }
        // 行全体で切り替えるため、スイッチ自体の操作は受けない
        Switch(checked = checked, onCheckedChange = null)
    }
}

