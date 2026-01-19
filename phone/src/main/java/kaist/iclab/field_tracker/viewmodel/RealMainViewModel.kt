package kaist.iclab.field_tracker.viewmodel

import android.app.Activity
import kaist.iclab.tracker.TrackerUtil
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.controller.Controller
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.storage.core.SensorDataStorage
import kotlinx.coroutines.flow.StateFlow

class RealMainViewModel(
    private val controller: Controller,
    private val permissionManager: PermissionManager,
    private val authentication: Authentication,
    override val sensorDataStorages: List<SensorDataStorage>
) : MainViewModel() {
    override val sensors = controller.sensors
    override val controllerStateFlow = controller.controllerStateFlow
    override fun start() {
        controller.start()
    }

    override fun stop() {
        controller.stop()
    }

    override val userStateFlow = authentication.userStateFlow
    override suspend fun login(activity: Activity) {
        authentication.login(activity)
    }

    override suspend fun logout() {
        authentication.logout()
    }
//    override fun selectExperimentGroup(name: String) { TODO() }

    override val permissionStateFlow: StateFlow<Map<String, PermissionState>> =
        permissionManager.permissionStateFlow

    override fun requestPermissions(names: Array<String>, onResult: ((Boolean) -> Unit)?) {
        permissionManager.request(names) {
            onResult?.invoke(it)
        }
    }

    override fun getDeviceInfo(): String = TrackerUtil.getDeviceModel()
    override fun getAppVersion(): String = TrackerUtil.getAppVersion()
}