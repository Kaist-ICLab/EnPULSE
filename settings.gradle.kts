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
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "android-tracker"

// Include the Samsung Libraries
include(":samsung-health-data-api")
include(":samsung-health-sensor-api")

// Main Modules
include(":tracker-library")
include(":app-wearable-tracker")
include(":app-mobile-tracker")

// Test Modules
//include(":test-controller")
//include(":test-galaxywatch-sensor")
//include(":test-listener")
//include(":test-notification")
//include(":test-permission")
//include(":test-sensor")
//include(":test-survey")
//include(":test-sync")
//include(":test-sync-watch")
include(":test-survey")

// Other Modules
//include(":phone")
//include(":galaxywatch-monitor")
//include(":galaxywatch")
