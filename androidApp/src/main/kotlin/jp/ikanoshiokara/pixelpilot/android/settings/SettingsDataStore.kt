package jp.ikanoshiokara.pixelpilot.android.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 設定・前回のホストを保存する DataStore。プロセス内で 1 つだけ作る(同じファイルを複数の DataStore で開けないため)。
 * [DataStoreHostRepository] と [DataStoreSettingsRepository] が同じファイルを共有する。キーの一覧は各リポジトリに置く。
 */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
