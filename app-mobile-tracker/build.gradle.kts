plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.googleServices)

    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.2.10"
}

android {
    namespace = "kaist.iclab.mobiletracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "kaist.iclab.trackerSystem"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
            // Skip login is now disabled - require real authentication
            buildConfigField("Boolean", "SKIP_LOGIN", "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("Boolean", "SKIP_LOGIN", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    /* Android Tracker Library */
    implementation(project(":tracker-library"))

    /* Android Compose */
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    
    implementation("androidx.compose.material:material:1.9.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.7")

    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)

    /* Androidx */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.lifecycle.viewmodel)
    
    /* Navigation */
    implementation(libs.compose.navigation)

    /* Supabase Related */
    implementation(libs.supabase.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.postgrest.kt)
    implementation(libs.realtime.kt)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)
    
    /* Google Authentication (for Supabase Auth) */
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.auth)
    implementation(libs.googleid)
    
    /* Kotlin Serialization */
    implementation(libs.kotlinx.serialization.json)
    
    /* Koin Dependency Injection */
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    
    // RoomDB
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.gson) // for converter

    /* Google Play Services Wearable */
    implementation(libs.android.gms.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    
    /* Google Play Services Location */
    implementation(libs.android.gms.location)
    
}
