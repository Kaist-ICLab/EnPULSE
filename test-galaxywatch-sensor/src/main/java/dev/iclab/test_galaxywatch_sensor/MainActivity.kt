package dev.iclab.test_galaxywatch_sensor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.iclab.test_galaxywatch_sensor.ui.theme.AndroidtrackerTheme
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.tracker.sensor.galaxywatch.HRSensor
import kaist.iclab.tracker.sensor.galaxywatch.PPGSensor
import kaist.iclab.tracker.sensor.galaxywatch.SkinTempSensor
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    private val permissionManager by lazy {
        AndroidPermissionManager(this.applicationContext)
    }
    private val samsungHealthSensorInitializer by lazy {
        SamsungHealthSensorInitializer(this.applicationContext)
    }
    private val couchbaseDB by lazy {
        CouchbaseDB(this.applicationContext)
    }

    private val accelerometerSensor by lazy {
        AccelerometerSensor(
            this.applicationContext,
            permissionManager,
            CouchbaseStateStorage<AccelerometerSensor.Config>(
                couchbaseDB,
                AccelerometerSensor.Config(),
                AccelerometerSensor.Config::class.java,
                "accelerometer_config"
            ),
            CouchbaseStateStorage<SensorState>(
                couchbaseDB,
                SensorState(SensorState.FLAG.ENABLED),
                SensorState::class.java,
                "accelerometer_state",
            ),
            samsungHealthSensorInitializer
        )
    }
    private val hrSensor by lazy {
        HRSensor(
            this.applicationContext,
            permissionManager,
            CouchbaseStateStorage<HRSensor.Config>(
                couchbaseDB,
                HRSensor.Config(),
                HRSensor.Config::class.java,
                "hr_config"
            ),
            CouchbaseStateStorage<SensorState>(
                couchbaseDB,
                SensorState(SensorState.FLAG.ENABLED),
                SensorState::class.java,
                "hr_state",
            ),
            samsungHealthSensorInitializer
        )
    }

    private val ppgSensor by lazy {
        PPGSensor(
            this.applicationContext,
            permissionManager,
            CouchbaseStateStorage<PPGSensor.Config>(
                couchbaseDB,
                PPGSensor.Config(),
                PPGSensor.Config::class.java,
                "ppg_config"
            ),
            CouchbaseStateStorage<SensorState>(
                couchbaseDB,
                SensorState(SensorState.FLAG.ENABLED),
                SensorState::class.java,
                "ppg_state",
            ),
            samsungHealthSensorInitializer
        )
    }

    private val skinTempSensor by lazy {
        SkinTempSensor(
            this.applicationContext,
            permissionManager,
            CouchbaseStateStorage<SkinTempSensor.Config>(
                couchbaseDB,
                SkinTempSensor.Config(),
                SkinTempSensor.Config::class.java,
                "skintemp_config"
            ),
            CouchbaseStateStorage<SensorState>(
                couchbaseDB,
                SensorState(SensorState.FLAG.ENABLED),
                SensorState::class.java,
                "skintemp_state",
            ),
            samsungHealthSensorInitializer
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidtrackerTheme {
                SensorStatusScreen(
                    samsungHealthSensorInitializer.connectionStateFlow, mapOf(
                        "ACC" to accelerometerSensor,
                        "HR" to hrSensor,
                        "PPG" to ppgSensor,
                        "SkinTemp" to skinTempSensor
                    )
                )
            }
        }
    }
}


@Composable
fun SensorStatusScreen(
    connectionStateFlow: StateFlow<Boolean>,
    sensors: Map<String, Sensor<*, *>>
) {
    val connectionState = connectionStateFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp)
    ) {
        Row {
            Text(
                text = "Connection: ${if (connectionState.value) "Connected" else "Disconnected"}",
                fontSize = 10.sp
            )
            Button(onClick = {
                sensors.forEach { (_, sensor) ->
                    sensor.addListener { it ->
                        Log.d("DATA", it.toString())
                    }
                }
            }) {
                Text("init")
            }

        }

        sensors.forEach { (sensorName, sensor) ->
            val sensorState = sensor.sensorStateFlow.collectAsState()
            SensorControlItem(
                sensorName,
                sensorState.value.flag == SensorState.FLAG.RUNNING,
                sensorState.value.flag.toString(),
                { sensor.start() },
                { sensor.stop() })
        }
    }
}


@Composable
fun SensorControlItem(
    sensorName: String,
    isRunning: Boolean,
    state: String,
    start: () -> Unit,
    stop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = sensorName, fontSize = 14.sp)
        Text(text = state, fontSize = 14.sp)
        Button(onClick = { if (isRunning) stop() else start() }) {
            Text(if (isRunning) "Stop" else "Start", fontSize = 10.sp)
        }
    }
}