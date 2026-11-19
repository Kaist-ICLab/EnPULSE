// Samsung Health Sensor SDK sensors (heart rate, PPG, ECG, EDA, skin temperature,
// accelerometer, and stress) for the watch app. Included only when
// samsung-health-sensor-api/samsung-health-sensor-api.aar is present (see
// settings.gradle.kts). Distributing an app built with this module needs a Samsung
// Health partnership.
plugins {
    id("dev.iclab.android.basic.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "kaist.iclab.tracker.samsung.sensor"
}

dependencies {
    api(project(":tracker-library"))
    api(project(":samsung-health-sensor-api"))

    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.icons.extended)

    testImplementation(libs.junit)
}
