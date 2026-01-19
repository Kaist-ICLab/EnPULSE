package kaist.iclab.field_tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.SensorUIModel
import kaist.iclab.field_tracker.ui.components.BaseRow
import kaist.iclab.field_tracker.ui.components.DurationInputModalRow
import kaist.iclab.field_tracker.ui.components.Header
import kaist.iclab.field_tracker.ui.components.ListCard
import kaist.iclab.field_tracker.ui.components.SensorSwitchRow
import kaist.iclab.field_tracker.ui.theme.MainTheme
import kaist.iclab.tracker.permission.Permission
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.SampleSensor
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.full.memberProperties


@Composable
fun DataConfigScreen(
    sensor: SensorUIModel,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    permissionMap: Map<String, PermissionState>,
    onPermissionRequest: (Array<String>, (Boolean) -> Unit) -> Unit,
    lastUpdated: String,
    recordCount: String
    /*TODO: DataLayer Stat 처리: lastUpdated, record Count*/
) {
    val sensorConfig = sensor.configStateFlow.collectAsState().value
    val sensorState = sensor.sensorStateFlow.collectAsState().value
    Scaffold(
        topBar = {
            Header(
                title = "${sensor.name} configuration",
                canNavigateBack = canNavigateBack,
                navigateBack = navigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ListCard(
                rows = listOf({ SensorSwitchRow(sensor = sensor) })
            )
            ListCard(
                title = "Permissions",
                rows = if (permissionMap.size == 0) listOf(
                    {
                        Text(
                            "No permissions required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                )
                else permissionMap.map { (name, permissionState) ->
                    {
                        val permission =
                            Permission.supportedPermissions.find { it.ids[0] == name }
                                ?: error("Permission not found")
                        PermissionStateSwitchRow(
                            permission,
                            permissionState,
                            onPermissionRequest = { names -> onPermissionRequest(names, {}) }
                        )
                    }
                }
            )

            ListCard(
                title = "configs",
                rows = sensor.configClass.memberProperties.map { property ->
                    {
                        val currValue = property.getter.call(sensorConfig)?.toString()
                            ?: error("Value for ${property.name} is null")
                        when (property.returnType.classifier) {
                            /*TODO: Change it to more specific type (e.g., duration, category) considering formatting*/
                            in setOf(Int::class, Long::class) -> {
                                DurationInputModalRow(
                                    title = property.name,
                                    curValue = currValue,
                                    onValueChanged = {
                                        sensor.updateConfig(mapOf(property.name to it))
                                    },
                                    enabled = !(sensorState.flag in setOf(
                                        SensorState.FLAG.RUNNING,
                                    ))
                                )
                            }
                        }
                    }
                }
            )
            Box {
                ListCard(
                    title = "stats",
                    rows = listOf(
                        {
                            BaseRow("Last Record", subtitle = lastUpdated)
                        },
                        {
                            BaseRow("Records", subtitle = "${recordCount} Records")
                        },
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(color = Color.White.copy(alpha = .6F))
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DataConfigScreenPreview() {
    MainTheme {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DataConfigScreen(
                onPermissionRequest = { _, _ -> },
                permissionMap = mapOf(
                    "Location" to PermissionState.GRANTED,
                    "Activity" to PermissionState.PERMANENTLY_DENIED,
                    "Microphone" to PermissionState.RATIONALE_REQUIRED
                ),
                sensor = SensorUIModel(
                    id = "sensor_id",
                    name = "Sensor Name",
                    permissions = arrayOf("Location", "Activity", "Microphone"),
                    sensorStateFlow = MutableStateFlow(
                        SensorState(
                            SensorState.FLAG.ENABLED,
                            "Sensor is enabled"
                        )
                    ),
                    configStateFlow = MutableStateFlow(SampleSensor.Config(1000)),
                    configClass = SampleSensor.Config::class,
                    updateConfig = { },
                    enable = { },
                    disable = { }
                ),
                canNavigateBack = true,
                navigateBack = {},
                lastUpdated = "2021-01-01 12:00:00 (UTC+0000)",
                recordCount = "1000"
            )
        }
    }
}