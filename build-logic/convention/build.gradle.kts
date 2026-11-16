plugins {
    `kotlin-dsl`
}

group = "jp.ikanoshiokara.pixelpilot.buildlogic"

dependencies {
    // プラグイン本体はルートの build.gradle.kts(apply false)でクラスパスに載せるため compileOnly
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "pixelpilot.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "pixelpilot.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
    }
}
