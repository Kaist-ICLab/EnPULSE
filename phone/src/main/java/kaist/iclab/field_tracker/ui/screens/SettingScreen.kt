package kaist.iclab.field_tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.SensorUIModel
import kaist.iclab.field_tracker.ui.components.BaseRow
import kaist.iclab.field_tracker.ui.components.Header
import kaist.iclab.field_tracker.ui.components.ListCard
import kaist.iclab.field_tracker.ui.components.NavigationRow
import kaist.iclab.field_tracker.ui.components.SelectOptionModalRow
import kaist.iclab.field_tracker.ui.components.SensorSwitchRow
import kaist.iclab.field_tracker.ui.components.SwitchRow
import kaist.iclab.field_tracker.ui.components.SwitchStatus
import kaist.iclab.field_tracker.ui.theme.MainTheme
import kaist.iclab.tracker.auth.User
import kaist.iclab.tracker.auth.UserState
import kaist.iclab.tracker.controller.ControllerState
import kaist.iclab.tracker.permission.PermissionState

@Composable
fun SettingScreen(
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    onNavigateToPermissionList: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToDataConfig: (name: String) -> Unit,
    controllerState: ControllerState,
    onControllerStateChange: (Boolean) -> Unit,
    sensors: List<SensorUIModel>,
    permissionMap: Map<String, PermissionState>,
    userState: UserState,
    deviceInfo: String,
    appVersion: String
) {
    Scaffold(
        topBar = {
            Header(
                title = "The Tracker",
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
                rows = listOf({
                    SwitchRow(
                        title = "Run Tracker",
                        subtitle = if (controllerState.flag == ControllerState.FLAG.DISABLED)
                            controllerState.message else controllerState.flag.toString(),
                        switchStatus = SwitchStatus(
                            isChecked = controllerState.flag == ControllerState.FLAG.RUNNING,
                            onCheckedChange = onControllerStateChange,
                            disabled = controllerState.flag == ControllerState.FLAG.DISABLED
                        )
                    )
                })
            )
            ListCard(
                title = "Data",
                rows = sensors.map { sensor ->
                    { SensorSwitchRow(sensor, onClick = { onNavigateToDataConfig(sensor.id) }) }
                }
            )

            ListCard(
                title = "Permission",
                rows = listOf(
                    { NavigationRow("Permissions", onClick = onNavigateToPermissionList) },
                )
            )
            ListCard(
                title = "Info",
                rows = listOf(
                    { BaseRow("App Version", subtitle = appVersion) },
                    { BaseRow("Device", subtitle = deviceInfo) },
                    {
                        BaseRow("License", showDivider = true) {
                            IconButton(
                                modifier = Modifier.size(48.dp),
                                onClick = { /*TODO*/ }
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                )
            )

            /*TODO: How to integrate external services*/
            ListCard(
                title = "External Service",
                rows = listOf(
                    { BaseRow("Devices", subtitle = "Galaxy Watch, Polar H10") },
                    { BaseRow("External Apps", subtitle = "Samsung Health, Google Connect") },
                ),
                disabled = true
            )

            ListCard(
                title = "Profile",
                rows = listOf(
                    {
                        NavigationRow(
                            "User",
                            subtitle = if (!userState.isLoggedIn) "Not Loggined" else (userState.user?.email
                                ?: ""),
                            onClick = onNavigateToUserProfile
                        )
                    },
                    {
                        var experimentGroup by remember { mutableStateOf("None") }
                        val experimentGroups = listOf("None", "Group A", "Group B", "Group C")
                        /*TODO: Consider Loading for network delay*/
                        SelectOptionModalRow(
                            "Experiment Group",
                            experimentGroup,
                            experimentGroups,
                            onOptionSelected = { experimentGroup = it }
                        )
                    },
                ),
                disabled = true
            )

            ListCard(
                title = "Server Sync",
                rows = listOf(
                    /*TODO: Connect w/ Server sync data layer*/
                    {
                        var networkType by remember { mutableStateOf("WiFi-only") }
                        val networkTypes = listOf("WiFi-only", "WiFi + Mobile Data")
                        SelectOptionModalRow(
                            "Network Type",
                            networkType,
                            networkTypes,
                            onOptionSelected = { networkType = it }
                        )
                    },
                    {
                        var syncFreq by remember { mutableStateOf("Do not Sync") }
                        val syncFreqCandidates =
                            listOf("Do not Sync", "Sync every hour", "Sync every 2 hours")
                        SelectOptionModalRow(
                            "Sync Frequency",
                            syncFreq,
                            syncFreqCandidates,
                            onOptionSelected = { syncFreq = it }
                        )
                    },
                ),
                disabled = true
            )
        }
    }

}

@Preview(showBackground = true, heightDp = 2000)
@Composable
fun SettingScreenPreview() {
    MainTheme {
        SettingScreen(
            canNavigateBack = false,
            navigateBack = {},
            onNavigateToPermissionList = {},
            onNavigateToUserProfile = {},
            onNavigateToDataConfig = {},
            controllerState = ControllerState(ControllerState.FLAG.DISABLED, "Disabled"),
            onControllerStateChange = {},
            sensors = listOf(),
            userState = UserState(
                true, User(
                    name = "John Doe",
                    email = "john.doe@example",
                )
            ),
            permissionMap = mapOf(
                "android.permission.ACCESS_FINE_LOCATION" to PermissionState.GRANTED,
                "android.permission.ACTIVITY_RECOGNITION" to PermissionState.GRANTED,
            ),
            deviceInfo = "SM-G991N",
            appVersion = "1.0.0"
        )
    }
}
