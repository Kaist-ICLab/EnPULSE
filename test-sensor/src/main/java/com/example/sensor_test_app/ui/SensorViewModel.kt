package com.example.sensor_test_app.ui

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.TrackerUtil.formatLocalDateTime
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.couchbase.CouchbaseSensorDataStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SensorViewModel(
    private val backgroundController: BackgroundController,
    private val permissionManager: AndroidPermissionManager,
    private val sensorDataStorages: Map<String, CouchbaseSensorDataStorage>,
) : ViewModel() {
    private val sensors = backgroundController.sensors

    val sensorMap = sensors.associateBy { it.name }
    val sensorState = sensors.associate { it.name to it.sensorStateFlow }
    val controllerState = backgroundController.controllerStateFlow

    // Cleanup listeners on pause setting
    private val _cleanupListenersOnPause = MutableStateFlow(true)
    val cleanupListenersOnPause: StateFlow<Boolean> = _cleanupListenersOnPause.asStateFlow()

    private val _latestSensorData = MutableStateFlow<Map<String, String>>(emptyMap())
    val latestSensorData: StateFlow<Map<String, String>> = _latestSensorData.asStateFlow()

    init {
        refreshLatestSensorData()
    }

    fun setCleanupListenersOnPause(cleanup: Boolean) {
        _cleanupListenersOnPause.value = cleanup
    }

    fun toggleSensor(sensorName: String) {
        val status = sensorState[sensorName]!!.value.flag
        Log.d(sensorName, "Previous Status: ${status.toString()}")
        val sensor = sensorMap[sensorName]!!

        when (status) {
            SensorState.FLAG.DISABLED -> {
                permissionManager.request(sensor.permissions)
                CoroutineScope(Dispatchers.IO).launch {
                    permissionManager.getPermissionFlow(sensor.permissions)
                        .collect { permissionMap ->
                            Log.d("SensorViewModel", "$permissionMap")
                            if (permissionMap.values.all { it == PermissionState.GRANTED }) {
                                sensor.enable()
                                this.cancel()
                            }
                        }
                }
            }

            SensorState.FLAG.ENABLED -> sensor.disable()
            else -> Unit
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startLogging() {
        Log.d(SensorViewModel::class.simpleName, "StartLogging()")
        backgroundController.start()
    }

    fun stopLogging() {
        backgroundController.stop()
    }

    fun onSensorDataReceived(sensorName: String, data: SensorEntity) {
        sensorDataStorages[sensorName]?.insert(data)
        refreshLatestSensorData(sensorName)
    }

    fun refreshLatestSensorData(sensorName: String? = null) {
        val targetSensors = sensorName?.let { listOf(it) } ?: sensorDataStorages.keys
        val updated = _latestSensorData.value.toMutableMap()

        targetSensors.forEach { name ->
            val latestJson = sensorDataStorages[name]
                ?.getRecentData(limit = 1)
                ?.firstOrNull()

            updated[name] = latestJson?.let(::formatStoredSensorData) ?: "No data collected yet"
        }

        _latestSensorData.value = updated
    }

    private fun formatStoredSensorData(rawJson: String): String {
        val json = runCatching { JSONObject(rawJson) }.getOrNull() ?: return rawJson
        val orderedKeys = buildList {
            if (json.has("timestamp")) add("timestamp")
            json.keys().forEach { key ->
                if (key != "received" && key != "timestamp") add(key)
            }
        }

        return orderedKeys.joinToString("\n") { key ->
            val value = when (key) {
                "timestamp" -> json.optLong(key).takeIf { it > 0 }?.formatLocalDateTime()
                    ?: json.opt(key).toDisplayValue()
                else -> json.opt(key).toDisplayValue()
            }
            "$key=$value"
        }.ifBlank { rawJson }
    }

    private fun Any?.toDisplayValue(): String {
        return when (this) {
            null, JSONObject.NULL -> ""
            is JSONArray -> toString()
            is JSONObject -> toString()
            else -> toString()
        }
    }
}
