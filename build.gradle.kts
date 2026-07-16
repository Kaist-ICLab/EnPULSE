// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinCompose) apply false

    // For app-wearable-tracker
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false

    // For app-mobile-tracker ObjectBox migration
    alias(libs.plugins.objectbox) apply false
}