package jp.ikanoshiokara.pixelpilot.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * 前回接続したホストを保存する。起動時やフォアグラウンド復帰時の自動接続に使う(docs/design/v2.0-standard.md 7.2)。
 *
 * 実装はプラットフォームごと(Android は DataStore、androidApp)。接続先の機器名はハードコードせず、ここに保存したアドレスを使う。
 */
interface HostRepository {
    /** 前回接続したホストの Bluetooth アドレス。保存されていなければ null。 */
    val lastHostAddress: Flow<String?>

    /** 接続できたホストを保存する。 */
    suspend fun saveLastHost(address: String)

    /** 保存したホストを消す(接続先を選び直すとき)。 */
    suspend fun clearLastHost()
}
