import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compose Multiplatform を使う KMP ライブラリモジュール(view 配下)共通の設定(`pixelpilot.kmp.compose`)。
 * `pixelpilot.kmp.library` の設定に加えて、Compose のコンパイラと基本の依存を追加する。
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pixelpilot.kmp.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.findLibrary("compose-mp-runtime").get())
                    implementation(libs.findLibrary("compose-mp-foundation").get())
                    implementation(libs.findLibrary("compose-mp-ui").get())
                    implementation(libs.findLibrary("compose-mp-material3").get())
                }
            }
        }
    }
}
