// Samsung Health Data SDK sensors (steps, sleep, exercise) for the phone app.
// Included only when samsung-health-data-api/samsung-health-data-api.aar is present
// (see settings.gradle.kts). Distributing an app built with this module needs a
// Samsung Health partnership.
plugins {
    id("dev.iclab.android.basic.library")
    /* Parceler (for Samsung Health Data SDK) */
    id("kotlin-parcelize")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "kaist.iclab.tracker.samsung.data"
}

dependencies {
    api(project(":tracker-library"))
    api(project(":samsung-health-data-api"))
    // The Samsung Health data SDK uses Gson at runtime but does not declare it.
    implementation(libs.gson)

    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.icons.extended)
}
