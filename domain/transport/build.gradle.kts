// 経路の抽象: InputTransport、PlatformTransports。実装は transport/* に置く。
plugins {
    id("pixelpilot.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.modelInput)
            api(projects.modelConnection)
        }
    }
}
