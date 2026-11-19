import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)

    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlinSerialization)

    // ObjectBox (uses kapt; must be applied after the Android + Kotlin plugins).
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
val samsungHealthAvailable = rootProject.findProject(":tracker-samsung-data") != null


android {
    namespace = "kaist.iclab.mobiletracker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    lint {
        abortOnError = false
        disable.add("ExtraTranslation")
    }

    defaultConfig {
        // Must match the watch app's. Researchers distributing a samsung build use the package
        // name registered with their own Samsung Health partnership.
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

        // Supabase credentials: read from local.properties or environment (for CI)
        val supabaseUrl: String = configValue("SUPABASE_URL") ?: "MISSING_SUPABASE_URL"
        val supabaseAnonKey: String = configValue("SUPABASE_ANON_KEY") ?: "MISSING_SUPABASE_ANON_KEY"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    // "open" builds without the Samsung Health SDK (no step, sleep or exercise data) and can be
    // distributed. "samsung" adds those sensors and needs the SDK AAR and a Samsung Health
    // partnership before it is distributed.
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("androidTest") {
        assets.directories.add("schemas")
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
            signingConfig = signingConfigs.getByName("debug")
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

androidComponents {
    beforeVariants { variant ->
        if (variant.productFlavors.contains("health" to "samsung") && !samsungHealthAvailable) {
            variant.enable = false
        }
    }
    onVariants { variant ->
        val suffix = if (variant.flavorName == "samsung") "-Samsung" else ""
        variant.outputs.forEach { output ->
            output.outputFileName.set("EnPULSE-Mobile$suffix.apk")
        }
    }
}

dependencies {
    /* Android Tracker Library */
    implementation(project(":tracker-library"))
    if (samsungHealthAvailable) {
        "samsungImplementation"(project(":tracker-samsung-data"))
    }

    /* Couchbase Lite - tracker-library exposes CouchbaseDB but not the underlying com.couchbase.lite
       types, needed directly here by CouchbaseWebAppStorage */
    implementation(libs.couchbase)

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
    implementation(libs.storage.kt)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)

    /* Security - Encrypted SharedPreferences */
    implementation(libs.androidx.security.crypto)

    /* WebView bridge (WebMessageListener) for EnPULSE webapps */
    implementation(libs.androidx.webkit)

    /* Kotlin Serialization */
    implementation(libs.kotlinx.serialization.json)

    /* Koin Dependency Injection */
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // ObjectBox (applied via the io.objectbox plugin, which adds the runtime + kapt processor)

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
