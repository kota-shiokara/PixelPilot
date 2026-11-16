// 入力の解釈: TouchpadInterpreter(タッチ点の列 → InputEvent)、KeyboardLayout(文字 → Usage と修飾キー)。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelInput)
            api(projects.modelSettings)
        }
    }
}
