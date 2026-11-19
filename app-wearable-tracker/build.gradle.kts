import java.util.Properties

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

// Load local.properties for local development
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configValue(name: String): String? =
    findProperty(name)?.toString() ?: localProperties.getProperty(name) ?: System.getenv(name)

// The Samsung Health module is included only when its SDK AAR is present (see settings.gradle.kts).
val samsungHealthAvailable = rootProject.findProject(":tracker-samsung-sensor") != null


android {
    namespace = "kaist.iclab.wearabletracker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Must match the phone app's, or the Wear Data Layer won't connect them. Researchers
        // distributing a samsung build use the package name registered with their own Samsung
        // Health partnership.
        applicationId = configValue("ENPULSE_APPLICATION_ID") ?: "kaist.iclab.trackerSystem"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        // Galaxy Watches and supported phones are ARM; bundling x86/x86_64 libraries only
        // inflated the APKs. armeabi-v7a stays for watches running a 32-bit userspace.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
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

    // "open" builds without the Samsung Health SDK (no heart rate, PPG, EDA, skin temperature,
    // accelerometer, stress or ECG) and can be distributed. "samsung" adds those sensors and needs
    // the SDK AAR and a Samsung Health partnership before it is distributed.
    flavorDimensions += "health"
    productFlavors {
        create("open") {
            dimension = "health"
            isDefault = !samsungHealthAvailable
        }
        create("samsung") {
            dimension = "health"
            isDefault = samsungHealthAvailable
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
    beforeVariants { variant ->
        if (variant.productFlavors.contains("health" to "samsung") && !samsungHealthAvailable) {
            variant.enable = false
        }
    }
    onVariants { variant ->
        val suffix = if (variant.flavorName == "samsung") "-Samsung" else ""
        variant.outputs.forEach { output ->
            output.outputFileName.set("EnPULSE-Watch$suffix.apk")
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
    if (samsungHealthAvailable) {
        "samsungImplementation"(project(":tracker-samsung-sensor"))
    }

    // kotlinx serialization (for JSON handling in BLE communication)
    implementation(libs.kotlinx.serialization.json)
}