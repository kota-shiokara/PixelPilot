// HID Report Descriptor と HidReportEncoder(InputEvent → レポートのバイト列)。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelInput)
        }
    }
}
