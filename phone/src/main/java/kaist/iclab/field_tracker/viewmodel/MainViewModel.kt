package kaist.iclab.field_tracker.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.auth.UserState
import kaist.iclab.tracker.controller.ControllerState
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.SensorDataStorage
import kotlinx.coroutines.flow.StateFlow

abstract class MainViewModel : ViewModel() {
    abstract val controllerStateFlow: StateFlow<ControllerState>
    abstract fun start()
    abstract fun stop()

    abstract val sensors: List<Sensor<*, *>>
    abstract val sensorDataStorages: List<SensorDataStorage>

    abstract fun getDeviceInfo(): String
    abstract fun getAppVersion(): String

    abstract val userStateFlow: StateFlow<UserState>
    abstract suspend fun login(activity: Activity)
    abstract suspend fun logout()
//    fun selectExperimentGroup(name: String)

    abstract val permissionStateFlow: StateFlow<Map<String, PermissionState>>
    abstract fun requestPermissions(names: Array<String>, onResult: ((Boolean) -> Unit)? = null)
}