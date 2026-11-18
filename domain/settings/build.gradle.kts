// 設定と前回のホストを読み書きするリポジトリのインターフェース。実装(DataStore)は androidApp に置く。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelSettings)
            api(projects.modelConnection)
            // 保存した値の変化を Flow で公開する
            api(libs.kotlinx.coroutines.core)
        }
    }
}
