package kaist.iclab.tracker.sensor.controller

import kotlinx.serialization.Serializable

@Serializable
data class ControllerState(
    val flag: FLAG,
    val message: String? = null
) {
    enum class FLAG {
        DISABLED, // The tracker is not ready to run
        READY, // The tracker is ready to run
        RUNNING, // The tracker is running
        PAUSED // The tracker is paused (e.g., watch not worn)
    }
}