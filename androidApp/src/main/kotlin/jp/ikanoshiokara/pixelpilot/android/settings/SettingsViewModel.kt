package jp.ikanoshiokara.pixelpilot.android.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.ikanoshiokara.pixelpilot.domain.settings.SettingsRepository
import jp.ikanoshiokara.pixelpilot.model.settings.AppSettings
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 共通の設定(マウス・アプリ)を画面に渡し、設定画面での変更を保存する。
 *
 * メイン画面(マウスの画面への反映、画面を点けたままにするか)と設定画面の両方で使う。
 * 読み込みが終わるまでは各型の既定値を出す。
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val mouseSettings: StateFlow<MouseSettings> =
        repository.mouseSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MouseSettings())

    val appSettings: StateFlow<AppSettings> =
        repository.appSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppSettings())

    fun setSensitivity(sensitivity: Float) = updateMouse {
        it.copy(sensitivity = sensitivity.coerceIn(MouseSettings.SENSITIVITY_RANGE))
    }

    fun setScrollDirection(direction: ScrollDirection) = updateMouse { it.copy(scrollDirection = direction) }

    fun setShowButtons(show: Boolean) = updateMouse { it.copy(showButtons = show) }

    fun setKeepScreenOn(keepScreenOn: Boolean) {
        viewModelScope.launch { repository.updateAppSettings { it.copy(keepScreenOn = keepScreenOn) } }
    }

    private fun updateMouse(transform: (MouseSettings) -> MouseSettings) {
        viewModelScope.launch { repository.updateMouseSettings(transform) }
    }

    private companion object {
        /** 画面の回転などで購読が一瞬切れても、読み込み直さないための猶予。 */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
