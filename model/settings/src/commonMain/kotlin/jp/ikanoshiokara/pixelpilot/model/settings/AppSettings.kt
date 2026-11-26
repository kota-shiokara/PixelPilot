package jp.ikanoshiokara.pixelpilot.model.settings

/**
 * ホストによらない、アプリ全体の設定(マウスの設定 [MouseSettings] 以外)。docs/design/v2.0-standard.md 9.
 *
 * @property keepScreenOn マウス・キーボードの画面を開いている間、画面を点けたままにするか(既定: オン、7.3)。
 *   v2.0 はバックグラウンド時の接続保持を行わないため、オフにすると画面のスリープで接続が切れることがある。
 */
data class AppSettings(
    val keepScreenOn: Boolean = true,
)
