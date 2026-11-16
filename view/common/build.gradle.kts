// 全画面で使う UI: テーマ(色・文字)、共通部品(接続状態の表示など)、文字列リソース。
plugins {
    id("pixelpilot.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelConnection)
        }
    }
}
