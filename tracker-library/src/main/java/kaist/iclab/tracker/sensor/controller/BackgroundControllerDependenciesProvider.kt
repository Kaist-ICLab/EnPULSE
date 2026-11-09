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
    /**
     * Called after collection could not start or restart and has been stopped, for example when
     * Android refuses microphone or location access to a restart in the background. Use it to
     * tell the participant, since nothing else on screen shows that collection ended.
     */
    val onCollectionStartFailed: () -> Unit = {},
)

interface BackgroundControllerDependenciesProvider {
    fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies
}
