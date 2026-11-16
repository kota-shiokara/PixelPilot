// 各モジュール共通のビルド設定(convention plugin)を置く included build。
// ルートの settings.gradle.kts から includeBuild("build-logic") で読み込む。
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
