pluginManagement {
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

// モジュール間の依存を projects.shared.core のように型安全に書く
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// v1.0 のアプリ。v2.0 の androidApp に置き換えるまで残す(#30)
include(":app")

// v2.0(KMP)。構成は docs/design/v2.0.md 7. モジュール構成を参照
include(":androidApp")
include(":shared:core")
include(":shared:hid")
include(":shared:ui-components")
include(":shared:transport:api")
include(":shared:transport:bluetooth-hid")
