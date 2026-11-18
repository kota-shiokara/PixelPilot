package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.di

import jp.ikanoshiokara.pixelpilot.domain.transport.PlatformTransports
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.AndroidPlatformTransports
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Bluetooth HID 経路の Koin モジュール。各アプリ(androidApp)の `appModules` に並べる。
 *
 * BluetoothHidController は前回のホストの保存に HostRepository(androidApp が提供)を使う。
 * 追加予定: InputTransport の実装(#38)
 */
val bluetoothHidModule = module {
    single { AndroidPlatformTransports(androidContext()) } bind PlatformTransports::class
    single { BluetoothHidController(androidContext(), get(), get()) }
}
