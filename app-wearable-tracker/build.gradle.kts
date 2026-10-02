plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)

    // ObjectBox (uses kapt; must be applied after the Android + Kotlin plugins)
    alias(libs.plugins.kapt)
    alias(libs.plugins.objectbox)
}

kotlin {
    jvmToolchain(17)
}


// Version from git, so each of the demo devices shows which build it runs:
// versionCode = commit count, versionName = "1.0.0-<short hash>" (check with `adb shell dumpsys package kaist.iclab.trackerSystem | grep versionName`).
// Falls back to 1 / "1.0.0" when git is unavailable (e.g. a source zip).
fun gitOutput(vararg args: String): String? = runCatching {
    providers.exec { commandLine("git", *args) }.standardOutput.asText.get().trim().ifEmpty { null }
}.getOrNull()
val gitCommitCount = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val gitShortHash = gitOutput("rev-parse", "--short", "HEAD")

android {
    namespace = "kaist.iclab.wearabletracker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kaist.iclab.trackerSystem"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = gitCommitCount
        versionName = if (gitShortHash != null) "1.0.0-$gitShortHash" else "1.0.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = project.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildToolsVersion = libs.versions.buildTools.get()
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("EnPULSE-Watch.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.input)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.foundation.layout)
//    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Google Play Services
    implementation(libs.android.gms.wearable)
    implementation(libs.android.gms.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // icons
    implementation(libs.compose.material.icons.extended)

    // tracker library
    implementation(project(":tracker-library"))

    // kotlinx serialization (for JSON handling in BLE communication)
    implementation(libs.kotlinx.serialization.json)
}