// v2.0 の Android アプリ。画面遷移・ホスト選択・設定画面など、Android 固有の骨格を置く。
// UI 部品やロジックは shared/ 配下のモジュールに置く。
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "jp.ikanoshiokara.pixelpilot.android"
    compileSdk = 35

    defaultConfig {
        // v1.0 からアップデートできるよう applicationId を引き継ぐ(#30)。
        // そのため v1.0(app モジュール)と同じ端末に同時にはインストールできない。
        applicationId = "jp.ikanoshiokara.pixelpilot"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.shared.core)
    implementation(projects.shared.hid)
    implementation(projects.shared.uiComponents)
    implementation(projects.shared.transport.api)
    implementation(projects.shared.transport.bluetoothHid)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
}
