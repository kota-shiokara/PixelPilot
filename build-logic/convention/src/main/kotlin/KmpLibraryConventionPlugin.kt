import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP ライブラリモジュール共通の設定(`pixelpilot.kmp.library`)。
 *
 * - ターゲット: v2.0 は Android のみ。iOS・デスクトップ(jvm)は v2.x でここに追加する。
 * - 単体テストは commonTest に書き、Android の JVM 単体テスト(`./gradlew test`)で実行する。
 * - Android の名前空間はモジュールのパスから決める(例: `:model-input` → `jp.ikanoshiokara.pixelpilot.model.input`)。
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.library")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    @OptIn(ExperimentalKotlinGradlePluginApi::class)
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_11)
                    }
                }

                sourceSets.getByName("commonTest").dependencies {
                    implementation(libs.findLibrary("kotlin-test").get())
                }
            }

            extensions.configure<LibraryExtension> {
                namespace = "jp.ikanoshiokara.pixelpilot." +
                    path.removePrefix(":").replace('-', '.')
                compileSdk = 35
                defaultConfig {
                    minSdk = 28
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }
        }
    }
}
