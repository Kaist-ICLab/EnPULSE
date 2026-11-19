@file:Suppress("UnstableApiUsage")

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
    includeBuild("build-logic")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-tracker"

// Samsung Health modules. Their SDK AARs are not in the repository: Samsung licenses them for
// development only and needs a partnership before an app built with them is distributed. Each
// module is included only when its AAR is present, so the project builds without them; the apps
// then build the "open" flavor only. Pass -Penpulse.samsungHealth=false to leave them out even
// when the AARs are present.
val samsungHealthEnabled = providers.gradleProperty("enpulse.samsungHealth").orNull != "false"

if (samsungHealthEnabled && file("samsung-health-data-api/samsung-health-data-api.aar").exists()) {
    include(":samsung-health-data-api")
    include(":tracker-samsung-data")
}
if (samsungHealthEnabled && file("samsung-health-sensor-api/samsung-health-sensor-api.aar").exists()) {
    include(":samsung-health-sensor-api")
    include(":tracker-samsung-sensor")
}

// Main Modules
include(":tracker-library")
include(":app-wearable-tracker")
include(":app-mobile-tracker")
