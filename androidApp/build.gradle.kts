// v2.0 の Android アプリ。画面遷移・ホスト選択・設定画面など、Android 固有の骨格を置く。
// UI 部品は view/*、ロジックは domain/*、データ型は model/*、経路の実装は transport/* に置く。
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI(デバッグ版の配布、docs/distribution.md)から渡すもの。手元のビルドでは無くてよい

/** 配布するたびに増やす番号。CI は main のコミット数を渡す。 */
val versionCodeOverride = providers.gradleProperty("pixelpilot.versionCode").orNull?.toInt()

/** どのコードのビルドか(短いコミットのハッシュ)。版の名前に付ける。 */
val commit = providers.gradleProperty("pixelpilot.commit").orNull

/** デバッグ版に署名する鍵。配布した版の上に新しい版を入れられるよう、CI でも同じ鍵を使う。 */
val debugKeystore = providers.gradleProperty("pixelpilot.debugKeystore").orNull

android {
    namespace = "jp.ikanoshiokara.pixelpilot.android"
    compileSdk = 35

    defaultConfig {
        // v2.0 はまっさらな環境から始めるため、v1.0 からのアップデートは考えない
        applicationId = "jp.ikanoshiokara.pixelpilot"
        minSdk = 28
        targetSdk = 35
        versionCode = versionCodeOverride ?: 1
        versionName = "2.0.0"
        manifestPlaceholders["appLabel"] = "PixelPilot"
    }

    signingConfigs {
        getByName("debug") {
            // 既定のデバッグ鍵(~/.android/debug.keystore)と同じパスワード・別名で作った鍵を渡す
            debugKeystore?.let { storeFile = file(it) }
        }
    }

    buildTypes {
        getByName("debug") {
            // リリース版と同じ端末に並べて入れられるよう、アプリ ID と表示名を分ける
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug" + (commit?.let { "+$it" } ?: "")
            manifestPlaceholders["appLabel"] = "PixelPilot (debug)"
        }
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
    // androidApp は骨格として全レイヤーをつなぐ(経路の実装を Koin で view に注入する)
    implementation(projects.modelInput)
    implementation(projects.modelConnection)
    implementation(projects.modelSettings)
    implementation(projects.domainInput)
    implementation(projects.domainHid)
    implementation(projects.domainTransport)
    implementation(projects.domainSettings)
    implementation(projects.viewCommon)
    implementation(projects.viewMouse)
    implementation(projects.viewKeyboard)
    implementation(projects.transportBluetoothHid)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)

    implementation(libs.koin.android)
    // 画面の ViewModel を Koin から受け取る(koinViewModel)
    implementation(libs.koin.androidx.compose)
    // 設定・前回のホストの保存(domain/settings の実装)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
}
