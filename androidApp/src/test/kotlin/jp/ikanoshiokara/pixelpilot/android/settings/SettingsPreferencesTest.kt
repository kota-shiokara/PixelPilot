package jp.ikanoshiokara.pixelpilot.android.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import jp.ikanoshiokara.pixelpilot.model.settings.AppSettings
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferencesTest {
    @Test
    fun defaultsWhenNothingIsSaved() {
        val mouse = emptyPreferences().toMouseSettings()
        assertEquals(MouseSettings(), mouse)
        // v2.1 からクリックボタンは既定で非表示(決定事項 18)、画面は既定で点けたまま(7.3)
        assertFalse(mouse.showButtons)
        assertTrue(emptyPreferences().toAppSettings().keepScreenOn)
    }

    @Test
    fun mouseSettingsRoundTrip() {
        val settings = MouseSettings(sensitivity = 1.5f, scrollDirection = ScrollDirection.TRADITIONAL, showButtons = true)
        val prefs = mutablePreferencesOf().apply { putMouseSettings(settings) }
        assertEquals(settings, prefs.toMouseSettings())
    }

    @Test
    fun appSettingsRoundTrip() {
        val settings = AppSettings(keepScreenOn = false)
        val prefs = mutablePreferencesOf().apply { putAppSettings(settings) }
        assertEquals(settings, prefs.toAppSettings())
    }

    @Test
    fun unreadableValuesFallBackToDefaults() {
        val prefs = mutablePreferencesOf(
            SettingsKeys.MOUSE_SCROLL_DIRECTION to "UNKNOWN",
            SettingsKeys.MOUSE_SENSITIVITY to 10f,
        )
        val mouse = prefs.toMouseSettings()
        assertEquals(ScrollDirection.NATURAL, mouse.scrollDirection)
        assertEquals(MouseSettings.SENSITIVITY_RANGE.endInclusive, mouse.sensitivity)
    }
}
