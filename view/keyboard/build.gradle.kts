// キーボード画面(アプリ内のキー配列、特殊キー・修飾キーの列)。
plugins {
    id("pixelpilot.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.viewCommon)
            implementation(projects.domainInput)
            implementation(projects.domainTransport)
        }
    }
}
