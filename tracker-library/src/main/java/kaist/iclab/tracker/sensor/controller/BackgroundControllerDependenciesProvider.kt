package kaist.iclab.tracker.sensor.controller

import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.StateStorage

data class BackgroundControllerDependencies(
    val controllerStateStorage: StateStorage<ControllerState>,
    val sensors: List<Sensor<*, *>>,
    val serviceNotification: BackgroundController.ServiceNotification,
    val allowPartialSensing: Boolean,
    val offBodyDetector: OffBodyDetector,
    /**
     * Whether Android may restart collection by itself (START_STICKY) after the app's process
     * was killed. When false, collection stays stopped until it is started again from the app.
     */
    val restartAfterProcessDeath: Boolean = true,
)

interface BackgroundControllerDependenciesProvider {
    fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies
}
