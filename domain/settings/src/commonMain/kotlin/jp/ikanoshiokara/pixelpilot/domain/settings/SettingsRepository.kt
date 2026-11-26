package jp.ikanoshiokara.pixelpilot.domain.settings

import jp.ikanoshiokara.pixelpilot.model.settings.AppSettings
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import kotlinx.coroutines.flow.Flow

/**
 * 設定を保存する(docs/design/v2.0-standard.md 4.2、7.3、9.)。
 *
 * 実装はプラットフォームごと(Android は DataStore、androidApp)。保存していない項目は各型の既定値を返す。
 *
 * 設定は次の 2 種類に分けて持つ。
 * - 共通の設定: どのホストにつないでいても同じもの(マウスの設定、アプリの設定)。このインターフェースで扱う
 * - ホストごとの設定: OS・キーボード配列など、接続先の PC ごとに変わるもの。ホストのアドレスをキーにして、
 *   `hostSettings(address)` / `updateHostSettings(address, …)` の形でここに足す(#46 の後半)
 */
interface SettingsRepository {
    /** マウスの設定。 */
    val mouseSettings: Flow<MouseSettings>

    /** アプリ全体の設定。 */
    val appSettings: Flow<AppSettings>

    /** マウスの設定を、今の値をもとに書き換える。 */
    suspend fun updateMouseSettings(transform: (MouseSettings) -> MouseSettings)

    /** アプリ全体の設定を、今の値をもとに書き換える。 */
    suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings)
}
