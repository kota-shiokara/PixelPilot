package jp.ikanoshiokara.pixelpilot.android.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import jp.ikanoshiokara.pixelpilot.domain.settings.HostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** アプリの設定を保存する DataStore。プロセス内で 1 つだけ作る(同じファイルを複数の DataStore で開けないため)。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** [HostRepository] の DataStore による実装。 */
class DataStoreHostRepository(context: Context) : HostRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val lastHostAddress: Flow<String?> = dataStore.data.map { it[LAST_HOST_ADDRESS] }

    override suspend fun saveLastHost(address: String) {
        dataStore.edit { it[LAST_HOST_ADDRESS] = address }
    }

    override suspend fun clearLastHost() {
        dataStore.edit { it.remove(LAST_HOST_ADDRESS) }
    }

    private companion object {
        val LAST_HOST_ADDRESS = stringPreferencesKey("last_host_address")
    }
}
