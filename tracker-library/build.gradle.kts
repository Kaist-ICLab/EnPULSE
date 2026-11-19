plugins {
    id("dev.iclab.android.basic.library")
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "kaist.iclab.tracker"
}

dependencies {
    implementation(kotlin("reflect"))
    /* Android Compose (for survey) */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    /* Local Database*/
    implementation(libs.couchbase)

    /* Location */
    implementation(libs.android.gms.location)

    /* Data sync */
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.android.gms.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    /* On-device model */
    implementation(libs.tensorflow.lite)

    // The Samsung Health SDK sensors live in tracker-samsung-data and tracker-samsung-sensor,
    // so this library builds without the Samsung AARs.

    // Testing
    testImplementation(libs.junit)
}
