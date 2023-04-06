import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.multiplatform)
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.compose)
}

group = "jp.ikanoshiokara"
version = "0.0.1"

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PixelPilot"
            packageVersion = "1.0.0"
        }
    }
}
