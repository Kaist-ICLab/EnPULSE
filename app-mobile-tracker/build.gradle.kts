import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.googleServices)

    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "kaist.iclab.mobiletracker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kaist.iclab.trackerSystem"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        //noinspection WrongGradleMethod
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Load local.properties for local development
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // Supabase credentials: read from local.properties or environment (for CI)
        val supabaseUrl: String = findProperty("SUPABASE_URL")?.toString()
            ?: localProperties.getProperty("SUPABASE_URL")
            ?: System.getenv("SUPABASE_URL")
            ?: "MISSING_SUPABASE_URL"

        val supabaseAnonKey: String = findProperty("SUPABASE_ANON_KEY")?.toString()
            ?: localProperties.getProperty("SUPABASE_ANON_KEY")
            ?: System.getenv("SUPABASE_ANON_KEY")
            ?: "MISSING_SUPABASE_ANON_KEY"

        val supabaseServiceRoleKey: String = findProperty("SUPABASE_SERVICE_ROLE_KEY")?.toString()
            ?: localProperties.getProperty("SUPABASE_SERVICE_ROLE_KEY")
            ?: System.getenv("SUPABASE_SERVICE_ROLE_KEY")
            ?: "MISSING_SUPABASE_SERVICE_ROLE_KEY"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SUPABASE_SERVICE_ROLE_KEY", "\"$supabaseServiceRoleKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("androidTest") {
        assets.srcDir("schemas")
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
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildToolsVersion = libs.versions.buildTools.get()
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

    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)

    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)

    /* Androidx */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.compose.lifecycle.viewmodel)

    /* Navigation */
    implementation(libs.compose.navigation)

    /* Supabase Related */
    implementation(libs.supabase.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.postgrest.kt)
    implementation(libs.functions.kt)
    implementation(libs.realtime.kt)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)

    /* Google Authentication (for Supabase Auth) */
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.auth)
    implementation(libs.googleid)

    /* Security - Encrypted SharedPreferences */
    implementation(libs.androidx.security.crypto)

    /* Kotlin Serialization */
    implementation(libs.kotlinx.serialization.json)

    /* Koin Dependency Injection */
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // RoomDB
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.gson) // for converter
    androidTestImplementation(libs.androidx.room.testing)

    /* Google Play Services Wearable */
    implementation(libs.android.gms.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    /* Google Play Services Location */
    implementation(libs.android.gms.location)

    /* Testing */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
