// マウス画面。タッチ点を domain/input の TouchpadInterpreter に渡し、InputTransport で送る。
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
