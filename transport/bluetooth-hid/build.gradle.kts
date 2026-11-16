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
    }
}
