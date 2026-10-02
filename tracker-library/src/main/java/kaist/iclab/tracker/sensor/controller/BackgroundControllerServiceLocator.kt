package kaist.iclab.tracker.sensor.controller

import kaist.iclab.tracker.sensor.controller.BackgroundController.ServiceNotification
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.StateStorage

object BackgroundControllerServiceLocator {
    lateinit var controllerStateStorage: StateStorage<ControllerState>
    lateinit var sensors: List<Sensor<*, *>>
    lateinit var serviceNotification: ServiceNotification
    lateinit var offBodyDetector: OffBodyDetector
    var allowPartialSensing: Boolean = false
}