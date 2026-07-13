package kaist.iclab.wearabletracker.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.data.DeviceInfo
import kaist.iclab.wearabletracker.ecg.EcgMeasurementActivity
import kaist.iclab.wearabletracker.helpers.PermissionCheckResult
import kaist.iclab.wearabletracker.helpers.PermissionHelper
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.ui.components.AutoSyncSettings
import kaist.iclab.wearabletracker.ui.components.DeviceStatusInfo
import kaist.iclab.wearabletracker.ui.components.EcgInstructionDialog
import kaist.iclab.wearabletracker.ui.components.FlushConfirmationDialog
import kaist.iclab.wearabletracker.ui.components.IconButton
import kaist.iclab.wearabletracker.ui.components.PermissionPermanentlyDeniedDialog
import kaist.iclab.wearabletracker.ui.components.SamsungHealthConnectionErrorScreen
import kaist.iclab.wearabletracker.ui.components.SdkPolicyErrorScreen
import kaist.iclab.wearabletracker.ui.components.SensorToggleChip
import kaist.iclab.wearabletracker.ui.components.SettingController
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    androidPermissionManager: AndroidPermissionManager,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val sensorMap = settingsViewModel.sensorMap
    val isCollecting = settingsViewModel.controllerState.collectAsState().value
    val sensorState = settingsViewModel.sensorState

    val sensorStates = sensorState.mapValues { it.value.collectAsState() }
    val activeCampaignSensorNames by settingsViewModel.activeCampaignSensorNames.collectAsState()
    val availableSensors = sensorStates.filter { (name, state) ->
        state.value.flag != SensorState.FLAG.UNAVAILABLE &&
            (activeCampaignSensorNames == null || name in activeCampaignSensorNames!!)
    }

    var showFlushDialog by remember { mutableStateOf(false) }
    var showPermissionPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var showEcgInstructionDialog by remember { mutableStateOf(false) }
    val ecgAvailable by settingsViewModel.ecgAvailable.collectAsState()

    /**
     * Helper function to handle notification permission check and execute action if granted.
     * Reduces code duplication across different features (upload, flush, startLogging).
     */
    fun handleNotificationPermissionCheck(onGranted: () -> Unit) {
        when (PermissionHelper.checkNotificationPermission(context, androidPermissionManager)) {
            PermissionCheckResult.Granted -> {
                onGranted()
            }

            PermissionCheckResult.PermanentlyDenied -> {
                showPermissionPermanentlyDeniedDialog = true
            }

            PermissionCheckResult.Requested -> {
                // Permission requested - user needs to grant it and try again
            }
        }
    }

    // Check if any sensor is enabled
    val hasEnabledSensors = sensorState.values.any { stateFlow ->
        val state = stateFlow.collectAsState().value
        state.flag == SensorState.FLAG.ENABLED || state.flag == SensorState.FLAG.RUNNING
    }

    // Samsung Health connection state
    val isSamsungHealthConnected by settingsViewModel.isSamsungHealthConnected.collectAsState()

    // SDK Policy Error state (dev mode not enabled on Health Platform)
    val hasSdkPolicyError by settingsViewModel.sdkPolicyError.collectAsState()

    // State for showing connection error when user tries to start without connection
    var showConnectionError by remember { mutableStateOf(false) }

    // Device information state
    var deviceInfo by remember { mutableStateOf(DeviceInfo()) }
    LaunchedEffect(Unit) {
        settingsViewModel.getDeviceInfo(context) { receivedDeviceInfo ->
            deviceInfo = receivedDeviceInfo
        }
        // Load last sync timestamp on startup
        settingsViewModel.refreshLastSyncTimestamp()

        // Check notification permission at app startup (will request if needed, but won't show dialog for permanent denial)
        // The permanent denial dialog will only show when user tries to perform an action
        PermissionHelper.checkNotificationPermission(context, androidPermissionManager)
    }

    // Observe last sync timestamp
    val lastSyncTimestamp by settingsViewModel.lastSyncTimestamp.collectAsState()

    // Observe dashboard data
    val totalRecordCount by settingsViewModel.totalRecordCount.collectAsState()
    val batteryLevel by settingsViewModel.batteryLevel.collectAsState()
    val recordingStartTime by settingsViewModel.recordingStartTime.collectAsState()
    val syncProgress by settingsViewModel.syncProgress.collectAsState()

    // Observe phone connection status
    val isPhoneConnected by settingsViewModel.isPhoneConnected.collectAsState()

    // Observe auto-sync data
    val autoSyncEnabled by settingsViewModel.autoSyncEnabled.collectAsState()
    val autoSyncInterval by settingsViewModel.autoSyncInterval.collectAsState()

    //UI
    when {
        hasSdkPolicyError -> {
            // Show error screen when SDK Policy Error (dev mode not enabled)
            SdkPolicyErrorScreen(
                onDismiss = { settingsViewModel.clearSdkPolicyError() }
            )
        }

        showConnectionError -> {
            // Show error screen when user tries to start without Samsung Health connection
            SamsungHealthConnectionErrorScreen(
                onRetry = { showConnectionError = false }
            )
        }

        else -> {
            // Always show main settings UI
            Scaffold(
                vignette = {
                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp),
                ) {
                    SettingController(
                        upload = {
                            handleNotificationPermissionCheck {
                                settingsViewModel.upload()
                            }
                        },
                        flush = {
                            handleNotificationPermissionCheck {
                                showFlushDialog = true
                            }
                        },
                        startLogging = {
                            handleNotificationPermissionCheck {
                                // Check Samsung Health connection first
                                if (!isSamsungHealthConnected) {
                                    showConnectionError = true
                                } else {
                                    settingsViewModel.startLogging()
                                }
                            }
                        },
                        stopLogging = { settingsViewModel.stopLogging() },
                        isCollecting = (isCollecting.flag == ControllerState.FLAG.RUNNING),
                        hasEnabledSensors = hasEnabledSensors
                    )
                    DeviceStatusInfo(
                        deviceInfo = deviceInfo,
                        lastSyncTimestamp = lastSyncTimestamp,
                        totalRecordCount = totalRecordCount,
                        batteryLevel = batteryLevel,
                        isRecording = (isCollecting.flag == ControllerState.FLAG.RUNNING),
                        recordingStartTime = recordingStartTime,
                        syncProgress = syncProgress,
                        isPhoneConnected = isPhoneConnected,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {

                        if (ecgAvailable) {
                            Button(
                                onClick = { showEcgInstructionDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.measure_ecg),
                                        style = MaterialTheme.typography.button,
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Measure ECG"
                                    )
                                }
                            }
                        }

                        // Auto-Sync Settings
                        AutoSyncSettings(
                            enabled = autoSyncEnabled,
                            onEnabledChange = { settingsViewModel.setAutoSyncEnabled(it) },
                            intervalMs = autoSyncInterval,
                            onIntervalChange = { settingsViewModel.setAutoSyncInterval(it) }
                        )


                        availableSensors.forEach { (name, _) ->
                            SensorToggleChip(
                                sensorId = name,
                                sensorStateFlow = sensorState[name]!!,
                                updateStatus = { status ->
                                    if (status) {
                                        androidPermissionManager.request(sensorMap[name]!!.permissions)
                                    }
                                    settingsViewModel.update(name, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    FlushConfirmationDialog(
        showDialog = showFlushDialog,
        onDismiss = { showFlushDialog = false },
        onConfirm = {
            settingsViewModel.flush(context)
            showFlushDialog = false
        }
    )

    // Permission Permanently Denied Dialog
    PermissionPermanentlyDeniedDialog(
        showDialog = showPermissionPermanentlyDeniedDialog,
        onDismiss = { showPermissionPermanentlyDeniedDialog = false },
        onOpenSettings = {
            PermissionHelper.openNotificationSettings(context)
            showPermissionPermanentlyDeniedDialog = false
        }
    )

    // ECG Instruction Dialog
    EcgInstructionDialog(
        showDialog = showEcgInstructionDialog,
        onDismiss = { showEcgInstructionDialog = false },
        onStart = {
            showEcgInstructionDialog = false
            androidPermissionManager.request(settingsViewModel.ecgPermissions)
            context.startActivity(Intent(context, EcgMeasurementActivity::class.java))
        }
    )
}

