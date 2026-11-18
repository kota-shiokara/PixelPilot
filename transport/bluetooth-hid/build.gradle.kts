// Bluetooth HID 経路の実装: BluetoothHidDevice の制御、送信キュー。Android 専用(androidMain)。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.domainTransport)
            implementation(projects.domainHid)
        }
        androidMain.dependencies {
            // Koin モジュール(di パッケージ)で androidContext() を使う
            implementation(libs.koin.android)
            // ContextCompat.registerReceiver(Bluetooth のオン / オフの通知)
            implementation(libs.androidx.core.ktx)
        }
    }
}
