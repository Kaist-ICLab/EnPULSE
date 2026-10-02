package kaist.iclab.tracker.sensor.controller

import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.StateStorage

data class BackgroundControllerDependencies(
    val controllerStateStorage: StateStorage<ControllerState>,
    val sensors: List<Sensor<*, *>>,
    val serviceNotification: BackgroundController.ServiceNotification,
    val allowPartialSensing: Boolean,
    val offBodyDetector: OffBodyDetector,
)

interface BackgroundControllerDependenciesProvider {
    fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies
}
