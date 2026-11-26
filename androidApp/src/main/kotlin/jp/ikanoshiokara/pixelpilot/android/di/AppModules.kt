package jp.ikanoshiokara.pixelpilot.android.di

import jp.ikanoshiokara.pixelpilot.android.hostselect.HostSelectViewModel
import jp.ikanoshiokara.pixelpilot.android.settings.DataStoreHostRepository
import jp.ikanoshiokara.pixelpilot.android.settings.DataStoreSettingsRepository
import jp.ikanoshiokara.pixelpilot.android.settings.SettingsViewModel
import jp.ikanoshiokara.pixelpilot.domain.settings.HostRepository
import jp.ikanoshiokara.pixelpilot.domain.settings.SettingsRepository
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.di.bluetoothHidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * androidApp 固有の定義。Android の実装をインターフェースに束縛する。
 */
val androidAppModule: Module = module {
    single<HostRepository> { DataStoreHostRepository(androidContext()) }
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    viewModelOf(::HostSelectViewModel)
    viewModelOf(::SettingsViewModel)
}

/**
 * アプリ全体で読み込む Koin モジュールの一覧。
 *
 * 各モジュールが提供する Koin モジュール(view 配下・transport 配下の各モジュールの `di` パッケージ)をここに並べる。
 * model・domain は Koin に依存しないため、ここには並ばない。
 *
 * 追加予定:
 * - `view/mouse`・`view/keyboard`: 画面ごとの状態を持つクラス(#40、#42)
 */
val appModules: List<Module> = listOf(
    androidAppModule,
    // Bluetooth HID 経路(PlatformTransports。InputTransport などは #36〜#38 で追加)
    bluetoothHidModule,
)
