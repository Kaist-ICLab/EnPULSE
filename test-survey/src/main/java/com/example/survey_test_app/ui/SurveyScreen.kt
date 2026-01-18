package com.example.survey_test_app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kaist.iclab.tracker.TrackerUtil.formatLocalDateTime
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun SensorScreen(
    modifier: Modifier = Modifier,
    mainViewModel: SurveyViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val isCollecting = mainViewModel.controllerState.collectAsState().value.flag == ControllerState.FLAG.RUNNING
    val schedules = mainViewModel.scheduledTimes.collectAsState().value

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                if(isCollecting) mainViewModel.stopLogging()
                else {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                        mainViewModel.startLogging()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = if(isCollecting) "Stop" else "Start",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = {
                mainViewModel.resetSchedule()
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = "Reset Survey Schedule",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        SensorTestRow(
            sensorName = "Survey",
            sensorState = mainViewModel.sensorState,
            toggleSensor = { mainViewModel.toggleSensor() },
            sensorValue = ""
        )

        Button(
            onClick = {
                mainViewModel.startSurveyActivity("test")
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = "Start Survey Activity",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                mainViewModel.startSurveyActivity("fixedTime")
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = "Start Another Survey Activity",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                mainViewModel.updateScheduledTime()
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = "View survey schedule",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn {
            items(schedules) {
                Text(
                    text = it.formatLocalDateTime(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SensorTestRow(
    sensorName: String,
    sensorState: StateFlow<SensorState>,
    toggleSensor: () -> Unit,
    sensorValue: String,
    modifier: Modifier = Modifier,
) {
    val sensorState = sensorState.collectAsState().value

    val isSensorEnabled = (sensorState.flag == SensorState.FLAG.RUNNING || sensorState.flag == SensorState.FLAG.ENABLED)
    val canModifySensorState = (sensorState.flag == SensorState.FLAG.DISABLED || sensorState.flag == SensorState.FLAG.ENABLED)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(15.dp)
    ) {
        Text(sensorName)
        Spacer(Modifier.width(10.dp))
        SmallSquareIconButton(
            icon = if(isSensorEnabled) Icons.Default.Check else Icons.Default.Close,
            enabled = canModifySensorState,
            onClick = toggleSensor
        )
        Spacer(Modifier.width(15.dp))
        Text(sensorValue)
    }
}

@Composable
fun SmallSquareIconButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .width(25.dp)
            .height(25.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .width(20.dp)
                .height(20.dp)
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun SensorTestPreview() {
//    AndroidtrackerTheme {
//        SensorTest()
//    }
//}

//@Preview(showBackground = true)
//@Composable
//fun SensorTestRowPreview() {
//    AndroidtrackerTheme {
//        SensorTestRow(
//            sensorName = "AmbientLight",
//            sensorState = SensorState.FLAG.RUNNING,
//            grantPermission = {},
//            startSensor = {},
//            stopSensor = {},
//            sensorValue = "Value",
//        )
//    }
//}
