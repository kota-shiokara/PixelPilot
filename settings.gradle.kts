pluginManagement {
    // 各モジュール共通のビルド設定(pixelpilot.kmp.library など)
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PixelPilot"

// モジュール間の依存を projects.modelInput のように型安全に書く
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// v2.0(KMP)。構成と依存のルールは docs/design/v2.0.md 7. モジュール構成を参照。
// ディレクトリは レイヤー/機能 の階層にし、プロジェクト名は「レイヤー-機能」にする。
// model/input と domain/input のように同じ名前のディレクトリがあるため、
// プロジェクト名(= Kotlin のモジュール名)が重ならないようにしている。
fun includeModule(dir: String) {
    val name = ":" + dir.replace('/', '-')
    include(name)
    project(name).projectDir = file(dir)
}

include(":androidApp")

includeModule("model/input")
includeModule("model/connection")
includeModule("model/settings")

includeModule("domain/input")
includeModule("domain/hid")
includeModule("domain/transport")
includeModule("domain/settings")

includeModule("view/common")
includeModule("view/mouse")
includeModule("view/keyboard")

includeModule("transport/bluetooth-hid")
