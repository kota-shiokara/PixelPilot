// 設定と前回のホストを読み書きするリポジトリのインターフェース。実装(DataStore)は androidApp に置く。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelSettings)
            api(projects.modelConnection)
        }
    }
}
