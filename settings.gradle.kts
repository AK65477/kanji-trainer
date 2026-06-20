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

rootProject.name = "kanji-trainer"

include(":app")
include(":core:model")
include(":core:database")
include(":core:srs")
include(":core:seed-importer")
include(":feature:kanji-srs")
// Phase 2:
// include(":feature:reflection")
// include(":feature:notification")
// include(":feature:stats")
// include(":feature:backup")
