package com.example.survey_test_app.ui

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.example.survey_test_app.MyBackgroundController
import com.example.survey_test_app.SurveyDataReceiver
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.storage.couchbase.CouchbaseSurveyScheduleStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class SurveyViewModel(
    private val backgroundController: MyBackgroundController,
    private val permissionManager: AndroidPermissionManager,
    private val surveyDataReceiver: SurveyDataReceiver,
    private val surveyScheduleStorage: CouchbaseSurveyScheduleStorage
): ViewModel() {
    private val surveySensor by inject<SurveySensor>(SurveySensor::class.java)
    val sensorState = surveySensor.sensorStateFlow
    val controllerState = backgroundController.controllerStateFlow

    private val _scheduledTimes = MutableStateFlow(emptyList<Long>())
    val scheduledTimes: StateFlow<List<Long>> = _scheduledTimes.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            backgroundController.controllerStateFlow.collect {
                Log.v(SurveyDataReceiver::class.simpleName, it.toString())
                if(it.flag == ControllerState.FLAG.RUNNING) surveyDataReceiver.startBackgroundCollection()
                else surveyDataReceiver.stopBackgroundCollection()
            }
        }
    }

    fun toggleSensor() {
        val status = sensorState.value.flag
        when(status) {
            SensorState.FLAG.DISABLED -> {
                permissionManager.request(surveySensor.permissions)
                CoroutineScope(Dispatchers.IO).launch {
                    permissionManager.getPermissionFlow(surveySensor.permissions).collect { permissionMap ->
                        Log.d("SensorViewModel", "$permissionMap")
                        if(permissionMap.values.all { it == PermissionState.GRANTED }) {
                            surveySensor.enable()
                            this.cancel()
                        }
                    }
                }
            }

            SensorState.FLAG.ENABLED -> surveySensor.disable()
            else -> Unit
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startLogging(){
        backgroundController.start()
    }

    fun stopLogging() {
        backgroundController.stop()
    }

    fun resetSchedule() {
        surveyScheduleStorage.resetSchedule()
    }

    fun startSurveyActivity(id: String) {
        surveySensor.openSurvey(id)
    }

    fun updateScheduledTime() {
        _scheduledTimes.value = surveyScheduleStorage.getAllScheduledTimes()
    }
}