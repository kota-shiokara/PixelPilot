package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.di

import jp.ikanoshiokara.pixelpilot.domain.transport.InputTransport
import jp.ikanoshiokara.pixelpilot.domain.transport.PlatformTransports
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.AndroidPlatformTransports
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidTransport
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Bluetooth HID 経路の Koin モジュール。各アプリ(androidApp)の `appModules` に並べる。
 *
 * - [PlatformTransports]: この端末で使える経路
 * - BluetoothHidController: 登録・接続の制御(ホスト選択画面から使う)。前回のホストの保存に HostRepository(androidApp が提供)を使う
 * - [InputTransport]: 入力の送信(マウス・キーボード画面から使う)
 */
val bluetoothHidModule = module {
    single { AndroidPlatformTransports(androidContext()) } bind PlatformTransports::class
    single { BluetoothHidController(androidContext(), get(), get()) }
    single { BluetoothHidTransport(get()) } bind InputTransport::class
}
