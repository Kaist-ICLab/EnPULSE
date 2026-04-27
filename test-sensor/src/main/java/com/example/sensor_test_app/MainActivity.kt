package com.example.sensor_test_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sensor_test_app.ui.SensorScreen
import com.example.sensor_test_app.ui.SensorViewModel
import com.example.sensor_test_app.ui.theme.AndroidtrackerTheme
import kaist.iclab.tracker.MetaData
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorEntity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "SensorTestApp"
    }

    private val metaData by inject<MetaData>()

    private val permissionManager by inject<AndroidPermissionManager>()

    private val sensors by inject<List<Sensor<*, *>>>(named("sensors"))
    private val sensorViewModel: SensorViewModel by viewModel()

    // Flag to track whether listeners are currently added
    private var listenersAdded = false

    private val listener = sensors.map { sensor ->
        { data: SensorEntity ->
            // Debug: Log all sensor data
            Log.d(TAG, "Data Received From Sensor: ${sensor.name}, Data: $data")
            sensorViewModel.onSensorDataReceived(sensor.name, data)
            Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager.bind(this)

        // Request POST_NOTIFICATIONS permission on app launch
        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            AndroidtrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SensorScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun logMetaData() {
        super.onResume()
        Log.d(TAG, "Device UUID: ${metaData.deviceUuid}")
        Log.d(TAG, "Device Name: ${metaData.deviceName}")
        Log.d(TAG, "Device Model: ${metaData.deviceModel}")
        Log.d(TAG, "OS Version: ${metaData.osVersion}")
        Log.d(TAG, "App Id: ${metaData.appId}")
        Log.d(TAG, "App Version: ${metaData.appVersionCode}")
        Log.d(TAG, "App Version Name: ${metaData.appVersionName}")
        Log.d(TAG, "Library Version: ${metaData.libVersion}")
    }

    private fun requestNotificationPermission() {
        // POST_NOTIFICATIONS permission is only required on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted")
            }
        } else {
            Log.i(
                "MainActivity",
                "POST_NOTIFICATIONS permission not required on Android ${Build.VERSION.SDK_INT}"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        logMetaData()
        setupSensorListeners()
    }

    private fun setupSensorListeners() {
        if (listenersAdded) return
        Log.d(TAG, "Adding listeners to ${sensors.size} sensors")
        for (sensorIdx in sensors.indices) {
            val currentSensor = sensors[sensorIdx]
            currentSensor.addListener(listener[sensorIdx])
        }
        listenersAdded = true
        Log.d(TAG, "All sensor listeners added successfully")
    }

    override fun onPause() {
        super.onPause()

        val shouldCleanup = sensorViewModel.cleanupListenersOnPause.value

        if (shouldCleanup && listenersAdded) {
            Log.d(TAG, "Cleaning up sensor listeners on pause")
            for (sensorIdx in sensors.indices) {
                sensors[sensorIdx].removeListener(listener[sensorIdx])
            }
            listenersAdded = false
            Log.d(TAG, "All sensor listeners removed")
        } else if (!shouldCleanup) {
            Log.d(TAG, "Keeping sensor listeners active (cleanup disabled)")
        }
    }
}
