package jp.ikanoshiokara.pixelpilot.android.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import jp.ikanoshiokara.pixelpilot.domain.settings.SettingsRepository
import jp.ikanoshiokara.pixelpilot.model.settings.AppSettings
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [SettingsRepository] の DataStore による実装。 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val mouseSettings: Flow<MouseSettings> =
        dataStore.data.map { it.toMouseSettings() }.distinctUntilChanged()

    override val appSettings: Flow<AppSettings> =
        dataStore.data.map { it.toAppSettings() }.distinctUntilChanged()

    override suspend fun updateMouseSettings(transform: (MouseSettings) -> MouseSettings) {
        dataStore.edit { it.putMouseSettings(transform(it.toMouseSettings())) }
    }

    override suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { it.putAppSettings(transform(it.toAppSettings())) }
    }
}

/**
 * 設定の保存に使うキー。
 *
 * 共通の設定は項目の名前をそのままキーにする。ホストごとの設定(#46 の後半)は、
 * `host.<アドレス>.<項目>`(例: `host.AA:BB:CC:DD:EE:FF.os`)のように、ホストのアドレスを含めたキーで同じファイルに保存する。
 * 値の型を変えるときは、読めない値を既定値として扱うようにし、キーの名前を変えない。
 */
internal object SettingsKeys {
    val MOUSE_SENSITIVITY = floatPreferencesKey("mouse_sensitivity")
    val MOUSE_SCROLL_DIRECTION = stringPreferencesKey("mouse_scroll_direction")
    val MOUSE_SHOW_BUTTONS = booleanPreferencesKey("mouse_show_buttons")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
}

/** 保存した値からマウスの設定を作る。保存していない・読めない項目は既定値にする。 */
internal fun Preferences.toMouseSettings(): MouseSettings {
    val default = MouseSettings()
    return MouseSettings(
        sensitivity = this[SettingsKeys.MOUSE_SENSITIVITY]
            ?.coerceIn(MouseSettings.SENSITIVITY_RANGE)
            ?: default.sensitivity,
        scrollDirection = this[SettingsKeys.MOUSE_SCROLL_DIRECTION]
            ?.let { name -> ScrollDirection.entries.firstOrNull { it.name == name } }
            ?: default.scrollDirection,
        showButtons = this[SettingsKeys.MOUSE_SHOW_BUTTONS] ?: default.showButtons,
    )
}

internal fun MutablePreferences.putMouseSettings(settings: MouseSettings) {
    this[SettingsKeys.MOUSE_SENSITIVITY] = settings.sensitivity
    // enum は名前で保存する(並び順を変えても読めるように)
    this[SettingsKeys.MOUSE_SCROLL_DIRECTION] = settings.scrollDirection.name
    this[SettingsKeys.MOUSE_SHOW_BUTTONS] = settings.showButtons
}

/** 保存した値からアプリの設定を作る。保存していない項目は既定値にする。 */
internal fun Preferences.toAppSettings(): AppSettings {
    val default = AppSettings()
    return AppSettings(
        keepScreenOn = this[SettingsKeys.KEEP_SCREEN_ON] ?: default.keepScreenOn,
    )
}

internal fun MutablePreferences.putAppSettings(settings: AppSettings) {
    this[SettingsKeys.KEEP_SCREEN_ON] = settings.keepScreenOn
}
