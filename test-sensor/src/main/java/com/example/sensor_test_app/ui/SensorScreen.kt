package com.example.sensor_test_app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun SensorScreen(
    modifier: Modifier = Modifier,
    mainViewModel: SensorViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val isCollecting =
        mainViewModel.controllerState.collectAsState().value.flag == ControllerState.FLAG.RUNNING
    val controllerStateValue = mainViewModel.controllerState.collectAsState().value
    val latestSensorData = mainViewModel.latestSensorData.collectAsState().value

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Sensors Test Application",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Data is also logged on logcat",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    if (isCollecting) mainViewModel.stopLogging()
                    else {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            mainViewModel.startLogging()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isCollecting) "Stop Sensors" else "Start Sensors",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pause Sensor When Minimized",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f),
                    color = if (isCollecting) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = mainViewModel.cleanupListenersOnPause.collectAsState().value,
                    onCheckedChange = { mainViewModel.setCleanupListenersOnPause(it) },
                    enabled = !isCollecting
                )
            }
        }
        item {
            Text(
                text = "Latest collected data appears below for each sensor as events arrive.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        items(
            items = mainViewModel.sensorState.toList(),
            key = { it.first }
        ) { (key, value) ->
            SensorTestRow(
                sensorName = key,
                sensorStateFlow = value,
                isControllerRunning = controllerStateValue.flag == ControllerState.FLAG.RUNNING,
                toggleSensor = { mainViewModel.toggleSensor(key) },
                sensorValue = latestSensorData[key] ?: "No data collected yet"
            )
        }
    }
}

@Composable
fun SensorTestRow(
    sensorName: String,
    sensorStateFlow: StateFlow<SensorState>,
    isControllerRunning: Boolean,
    toggleSensor: () -> Unit,
    sensorValue: String,
    modifier: Modifier = Modifier,
) {
    val currentSensorState = sensorStateFlow.collectAsState().value

    val isSensorEnabled =
        (currentSensorState.flag == SensorState.FLAG.RUNNING || currentSensorState.flag == SensorState.FLAG.ENABLED)

    Row(
        modifier = modifier
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sensorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                SmallSquareIconButton(
                    icon = if (isSensorEnabled) Icons.Default.Check else Icons.Default.Close,
                    enabled = !isControllerRunning,
                    onClick = toggleSensor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = sensorValue,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
