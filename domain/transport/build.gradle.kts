// 経路の抽象: InputTransport、PlatformTransports。実装は transport 配下のモジュールに置く。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelInput)
            api(projects.modelConnection)
            // 使える経路・接続状態の変化を StateFlow で公開する
            api(libs.kotlinx.coroutines.core)
        }
    }
}
