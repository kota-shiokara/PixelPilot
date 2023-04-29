@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

group = "jp.ikanoshiokara"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidxActivityCompose)
    implementation(libs.zxingAndroidEmbedded)
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "jp.ikanoshiokara.pixelpilot"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "$version"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}