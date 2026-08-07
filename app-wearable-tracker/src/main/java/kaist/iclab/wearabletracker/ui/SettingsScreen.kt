package kaist.iclab.wearabletracker.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.tooling.preview.devices.WearDevices
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.data.DeviceInfo
import kaist.iclab.wearabletracker.ecg.EcgMeasurementActivity
import kaist.iclab.wearabletracker.helpers.PermissionCheckResult
import kaist.iclab.wearabletracker.helpers.PermissionHelper
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.SensorNameText
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import kaist.iclab.wearabletracker.ui.components.DataActionsRow
import kaist.iclab.wearabletracker.ui.components.DeviceStatusInfo
import kaist.iclab.wearabletracker.ui.components.EcgInstructionDialog
import kaist.iclab.wearabletracker.ui.components.FlushConfirmationDialog
import kaist.iclab.wearabletracker.ui.components.PermissionPermanentlyDeniedDialog
import kaist.iclab.wearabletracker.ui.components.PhoneSyncRequiredScreen
import kaist.iclab.wearabletracker.ui.components.SamsungHealthConnectionErrorScreen
import kaist.iclab.wearabletracker.ui.components.SdkPolicyErrorScreen
import kaist.iclab.wearabletracker.ui.components.SensorToggleChip
import kaist.iclab.wearabletracker.ui.components.SettingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    androidPermissionManager: AndroidPermissionManager,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val sensorMap = settingsViewModel.sensorMap
    val controllerStateValue = settingsViewModel.controllerState.collectAsState().value
    val isPaused = controllerStateValue.flag == ControllerState.FLAG.PAUSED
    val sensorState = settingsViewModel.sensorState

    val sensorStates = sensorState.mapValues { it.value.collectAsState() }
    val activeCampaignSensorNames by settingsViewModel.activeCampaignSensorNames.collectAsState()
    val availableSensors = sensorStates.filter { (name, state) ->
        state.value.flag != SensorState.FLAG.UNAVAILABLE &&
            (activeCampaignSensorNames == null || name in activeCampaignSensorNames!!)
    }.keys.associateWith { name -> sensorState[name]!! }

    var showFlushDialog by remember { mutableStateOf(false) }
    var showPermissionPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var showEcgInstructionDialog by remember { mutableStateOf(false) }
    val ecgAvailable by settingsViewModel.ecgAvailable.collectAsState()

    /**
     * Helper function to handle notification permission check and execute action if granted.
     * Reduces code duplication across different features (upload, flush, startLogging).
     */
    fun handleNotificationPermissionCheck(onGranted: () -> Unit) {
        when (PermissionHelper.checkNotificationPermission(androidPermissionManager)) {
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
        PermissionHelper.checkNotificationPermission(androidPermissionManager)
    }

    // Observe last sync timestamp
    val lastSyncTimestamp by settingsViewModel.lastSyncTimestamp.collectAsState()

    // Observe dashboard data
    val totalRecordCount by settingsViewModel.totalRecordCount.collectAsState()

    // Pending upload counts arrive keyed by sensor id; the sensor list below is keyed by
    // sensor name, so re-key them here rather than making every call site translate.
    val pendingCountsBySensorId by settingsViewModel.pendingRecordCounts.collectAsState()
    val pendingRecordCounts = remember(pendingCountsBySensorId, sensorMap) {
        sensorMap.mapNotNull { (name, sensor) ->
            pendingCountsBySensorId[sensor.id]?.let { name to it }
        }.toMap()
    }
    val batteryLevel by settingsViewModel.batteryLevel.collectAsState()
    val recordingStartTime by settingsViewModel.recordingStartTime.collectAsState()
    val syncProgress by settingsViewModel.syncProgress.collectAsState()

    // Observe phone connection status
    val isPhoneConnected by settingsViewModel.isPhoneConnected.collectAsState()

    // Observe auto-sync interval (0 = off; there's no separate enabled flag in the UI)
    val autoSyncInterval by settingsViewModel.autoSyncInterval.collectAsState()

    SettingsScreenContent(
        hasNeverSynced = activeCampaignSensorNames == null,
        hasSdkPolicyError = hasSdkPolicyError,
        onDismissSdkPolicyError = { settingsViewModel.clearSdkPolicyError() },
        showConnectionError = showConnectionError,
        onRetryConnection = { showConnectionError = false },
        isCollecting = (controllerStateValue.flag == ControllerState.FLAG.RUNNING ||
                controllerStateValue.flag == ControllerState.FLAG.PAUSED),
        isPaused = isPaused,
        hasEnabledSensors = hasEnabledSensors,
        onUpload = { handleNotificationPermissionCheck { settingsViewModel.upload() } },
        onFlush = { handleNotificationPermissionCheck { showFlushDialog = true } },
        onStartLogging = {
            handleNotificationPermissionCheck {
                // Check Samsung Health connection first
                if (!isSamsungHealthConnected) {
                    showConnectionError = true
                } else {
                    settingsViewModel.startLogging()
                }
            }
        },
        onStopLogging = { settingsViewModel.stopLogging() },
        deviceInfo = deviceInfo,
        lastSyncTimestamp = lastSyncTimestamp,
        totalRecordCount = totalRecordCount,
        pendingRecordCounts = pendingRecordCounts,
        batteryLevel = batteryLevel,
        recordingStartTime = recordingStartTime,
        syncProgress = syncProgress,
        isPhoneConnected = isPhoneConnected,
        ecgAvailable = ecgAvailable,
        onMeasureEcgClick = { showEcgInstructionDialog = true },
        autoSyncInterval = autoSyncInterval,
        onAutoSyncIntervalChange = { settingsViewModel.setAutoSyncInterval(it) },
        availableSensors = availableSensors,
        onSensorToggle = { name, status ->
            if (status) {
                androidPermissionManager.request(sensorMap[name]!!.permissions)
            }
            settingsViewModel.update(name, status)
        },
        showFlushDialog = showFlushDialog,
        onDismissFlushDialog = { showFlushDialog = false },
        onConfirmFlush = {
            settingsViewModel.flush(context)
            showFlushDialog = false
        },
        showPermissionPermanentlyDeniedDialog = showPermissionPermanentlyDeniedDialog,
        onDismissPermissionDialog = { showPermissionPermanentlyDeniedDialog = false },
        onOpenNotificationSettings = {
            PermissionHelper.openNotificationSettings(context)
            showPermissionPermanentlyDeniedDialog = false
        },
        showEcgInstructionDialog = showEcgInstructionDialog,
        onDismissEcgInstructionDialog = { showEcgInstructionDialog = false },
        onStartEcgMeasurement = {
            showEcgInstructionDialog = false
            androidPermissionManager.request(settingsViewModel.ecgPermissions)
            context.startActivity(Intent(context, EcgMeasurementActivity::class.java))
        }
    )
}

/**
 * Stateless rendering of the settings screen — every value is a plain parameter and every
 * action a callback, so it doesn't need a ViewModel or AndroidPermissionManager and can be
 * exercised directly from @Preview.
 */
@Composable
fun SettingsScreenContent(
    hasNeverSynced: Boolean,
    hasSdkPolicyError: Boolean,
    onDismissSdkPolicyError: () -> Unit,
    showConnectionError: Boolean,
    onRetryConnection: () -> Unit,
    isCollecting: Boolean,
    isPaused: Boolean,
    hasEnabledSensors: Boolean,
    onUpload: () -> Unit,
    onFlush: () -> Unit,
    onStartLogging: () -> Unit,
    onStopLogging: () -> Unit,
    deviceInfo: DeviceInfo,
    lastSyncTimestamp: Long?,
    totalRecordCount: Int,
    pendingRecordCounts: Map<String, Int>,
    batteryLevel: Int,
    recordingStartTime: Long?,
    syncProgress: Float?,
    isPhoneConnected: Boolean,
    ecgAvailable: Boolean,
    onMeasureEcgClick: () -> Unit,
    autoSyncInterval: Long,
    onAutoSyncIntervalChange: (Long) -> Unit,
    availableSensors: Map<String, StateFlow<SensorState>>,
    onSensorToggle: (name: String, status: Boolean) -> Unit,
    showFlushDialog: Boolean,
    onDismissFlushDialog: () -> Unit,
    onConfirmFlush: () -> Unit,
    showPermissionPermanentlyDeniedDialog: Boolean,
    onDismissPermissionDialog: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    showEcgInstructionDialog: Boolean,
    onDismissEcgInstructionDialog: () -> Unit,
    onStartEcgMeasurement: () -> Unit,
) {
    when {
        hasNeverSynced -> {
            // Block the entire UI until the phone pushes campaign configuration
            PhoneSyncRequiredScreen()
        }

        hasSdkPolicyError -> {
            // Show error screen when SDK Policy Error (dev mode not enabled)
            SdkPolicyErrorScreen(onDismiss = onDismissSdkPolicyError)
        }

        showConnectionError -> {
            // Show error screen when user tries to start without Samsung Health connection
            SamsungHealthConnectionErrorScreen(onRetry = onRetryConnection)
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
                        .padding(top = 10.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    DeviceStatusInfo(
                        deviceInfo = deviceInfo,
                        lastSyncTimestamp = lastSyncTimestamp,
                        totalRecordCount = totalRecordCount,
                        batteryLevel = batteryLevel,
                        isRecording = isCollecting && !isPaused,
                        isPaused = isPaused,
                        recordingStartTime = recordingStartTime,
                        syncProgress = syncProgress,
                        isPhoneConnected = isPhoneConnected,
                    )
                    SettingController(
                        startLogging = onStartLogging,
                        stopLogging = onStopLogging,
                        isCollecting = isCollecting,
                        hasEnabledSensors = hasEnabledSensors,
                        autoSyncIntervalMs = autoSyncInterval,
                        onAutoSyncIntervalChange = onAutoSyncIntervalChange
                    )

                    if (ecgAvailable) {
                        Chip(
                            label = {
                                SensorNameText(
                                    text = stringResource(R.string.measure_ecg),
                                    color = MaterialTheme.colors.onPrimary
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Measure ECG",
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            },
                            onClick = onMeasureEcgClick,
                            colors = ChipDefaults.chipColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = AppSpacing.sensorChipHorizontal,
                                    end = AppSpacing.sensorChipHorizontal,
                                    bottom = AppSpacing.sensorChipBottom
                                ).height(AppSizes.sensorChipHeight),
                        )
                    }

                    availableSensors.forEach { (name, stateFlow) ->
                        SensorToggleChip(
                            sensorId = name,
                            sensorStateFlow = stateFlow,
                            isCollecting = isCollecting,
                            pendingRecordCount = pendingRecordCounts[name] ?: 0,
                            updateStatus = { status -> onSensorToggle(name, status) }
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    DataActionsRow(
                        upload = onUpload,
                        flush = onFlush
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    FlushConfirmationDialog(
        showDialog = showFlushDialog,
        onDismiss = onDismissFlushDialog,
        onConfirm = onConfirmFlush
    )

    // Permission Permanently Denied Dialog
    PermissionPermanentlyDeniedDialog(
        showDialog = showPermissionPermanentlyDeniedDialog,
        onDismiss = onDismissPermissionDialog,
        onOpenSettings = onOpenNotificationSettings
    )

    // ECG Instruction Dialog
    EcgInstructionDialog(
        showDialog = showEcgInstructionDialog,
        onDismiss = onDismissEcgInstructionDialog,
        onStart = onStartEcgMeasurement
    )
}

private fun previewSensorState(flag: SensorState.FLAG): StateFlow<SensorState> =
    MutableStateFlow(SensorState(flag))

private val previewAvailableSensors: Map<String, StateFlow<SensorState>> = linkedMapOf(
    "Accelerometer" to previewSensorState(SensorState.FLAG.RUNNING),
    "Heart Rate" to previewSensorState(SensorState.FLAG.ENABLED),
    "PPG" to previewSensorState(SensorState.FLAG.DISABLED),
    "Skin Temperature" to previewSensorState(SensorState.FLAG.DISABLED),
    "EDA" to previewSensorState(SensorState.FLAG.DISABLED),
)

private val previewPendingCounts: Map<String, Int> = mapOf(
    "Accelerometer" to 4820,
    "Heart Rate" to 137,
    "PPG" to 0,
)

@Preview(name = "Idle", device = WearDevices.SMALL_ROUND, showBackground = true)
@Preview(name = "Idle - Large Round", device = WearDevices.LARGE_ROUND, showBackground = true)
@Composable
private fun SettingsScreenContentIdlePreview() {
    WearableTrackerTheme {
        SettingsScreenContent(
            hasNeverSynced = false,
            hasSdkPolicyError = false,
            onDismissSdkPolicyError = {},
            showConnectionError = false,
            onRetryConnection = {},
            isCollecting = false,
            isPaused = false,
            hasEnabledSensors = true,
            onUpload = {},
            onFlush = {},
            onStartLogging = {},
            onStopLogging = {},
            deviceInfo = DeviceInfo(name = "Galaxy Watch6"),
            lastSyncTimestamp = System.currentTimeMillis() - 15 * 60_000L,
            totalRecordCount = 1234,
            pendingRecordCounts = previewPendingCounts,
            batteryLevel = 82,
            recordingStartTime = null,
            syncProgress = null,
            isPhoneConnected = true,
            ecgAvailable = true,
            onMeasureEcgClick = {},
            autoSyncInterval = 1_800_000L,
            onAutoSyncIntervalChange = {},
            availableSensors = previewAvailableSensors,
            onSensorToggle = { _, _ -> },
            showFlushDialog = false,
            onDismissFlushDialog = {},
            onConfirmFlush = {},
            showPermissionPermanentlyDeniedDialog = false,
            onDismissPermissionDialog = {},
            onOpenNotificationSettings = {},
            showEcgInstructionDialog = false,
            onDismissEcgInstructionDialog = {},
            onStartEcgMeasurement = {},
        )
    }
}

@Preview(name = "Recording", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun SettingsScreenContentRecordingPreview() {
    WearableTrackerTheme {
        SettingsScreenContent(
            hasNeverSynced = false,
            hasSdkPolicyError = false,
            onDismissSdkPolicyError = {},
            showConnectionError = false,
            onRetryConnection = {},
            isCollecting = true,
            isPaused = false,
            hasEnabledSensors = true,
            onUpload = {},
            onFlush = {},
            onStartLogging = {},
            onStopLogging = {},
            deviceInfo = DeviceInfo(name = "Galaxy Watch6"),
            lastSyncTimestamp = System.currentTimeMillis() - 15 * 60_000L,
            totalRecordCount = 5821,
            pendingRecordCounts = previewPendingCounts,
            batteryLevel = 24,
            recordingStartTime = System.currentTimeMillis() - 12 * 60_000L,
            syncProgress = 0.42f,
            isPhoneConnected = false,
            ecgAvailable = true,
            onMeasureEcgClick = {},
            autoSyncInterval = 300_000L,
            onAutoSyncIntervalChange = {},
            availableSensors = previewAvailableSensors,
            onSensorToggle = { _, _ -> },
            showFlushDialog = false,
            onDismissFlushDialog = {},
            onConfirmFlush = {},
            showPermissionPermanentlyDeniedDialog = false,
            onDismissPermissionDialog = {},
            onOpenNotificationSettings = {},
            showEcgInstructionDialog = false,
            onDismissEcgInstructionDialog = {},
            onStartEcgMeasurement = {},
        )
    }
}

@Preview(name = "Flush Confirmation Dialog", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun SettingsScreenContentFlushDialogPreview() {
    WearableTrackerTheme {
        SettingsScreenContent(
            hasNeverSynced = false,
            hasSdkPolicyError = false,
            onDismissSdkPolicyError = {},
            showConnectionError = false,
            onRetryConnection = {},
            isCollecting = false,
            isPaused = false,
            hasEnabledSensors = true,
            onUpload = {},
            onFlush = {},
            onStartLogging = {},
            onStopLogging = {},
            deviceInfo = DeviceInfo(name = "Galaxy Watch6"),
            lastSyncTimestamp = null,
            totalRecordCount = 0,
            pendingRecordCounts = previewPendingCounts,
            batteryLevel = 60,
            recordingStartTime = null,
            syncProgress = null,
            isPhoneConnected = true,
            ecgAvailable = false,
            onMeasureEcgClick = {},
            autoSyncInterval = 0L,
            onAutoSyncIntervalChange = {},
            availableSensors = previewAvailableSensors,
            onSensorToggle = { _, _ -> },
            showFlushDialog = true,
            onDismissFlushDialog = {},
            onConfirmFlush = {},
            showPermissionPermanentlyDeniedDialog = false,
            onDismissPermissionDialog = {},
            onOpenNotificationSettings = {},
            showEcgInstructionDialog = false,
            onDismissEcgInstructionDialog = {},
            onStartEcgMeasurement = {},
        )
    }
}

@Preview(name = "SDK Policy Error", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun SettingsScreenContentSdkPolicyErrorPreview() {
    WearableTrackerTheme {
        SettingsScreenContent(
            hasNeverSynced = false,
            hasSdkPolicyError = true,
            onDismissSdkPolicyError = {},
            showConnectionError = false,
            onRetryConnection = {},
            isCollecting = false,
            isPaused = false,
            hasEnabledSensors = false,
            onUpload = {},
            onFlush = {},
            onStartLogging = {},
            onStopLogging = {},
            deviceInfo = DeviceInfo(),
            lastSyncTimestamp = null,
            totalRecordCount = 0,
            pendingRecordCounts = emptyMap(),
            batteryLevel = -1,
            recordingStartTime = null,
            syncProgress = null,
            isPhoneConnected = false,
            ecgAvailable = false,
            onMeasureEcgClick = {},
            autoSyncInterval = 0L,
            onAutoSyncIntervalChange = {},
            availableSensors = emptyMap(),
            onSensorToggle = { _, _ -> },
            showFlushDialog = false,
            onDismissFlushDialog = {},
            onConfirmFlush = {},
            showPermissionPermanentlyDeniedDialog = false,
            onDismissPermissionDialog = {},
            onOpenNotificationSettings = {},
            showEcgInstructionDialog = false,
            onDismissEcgInstructionDialog = {},
            onStartEcgMeasurement = {},
        )
    }
}

@Preview(name = "Samsung Health Connection Error", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun SettingsScreenContentConnectionErrorPreview() {
    WearableTrackerTheme {
        SettingsScreenContent(
            hasNeverSynced = false,
            hasSdkPolicyError = false,
            onDismissSdkPolicyError = {},
            showConnectionError = true,
            onRetryConnection = {},
            isCollecting = false,
            isPaused = false,
            hasEnabledSensors = true,
            onUpload = {},
            onFlush = {},
            onStartLogging = {},
            onStopLogging = {},
            deviceInfo = DeviceInfo(),
            lastSyncTimestamp = null,
            totalRecordCount = 0,
            pendingRecordCounts = emptyMap(),
            batteryLevel = -1,
            recordingStartTime = null,
            syncProgress = null,
            isPhoneConnected = false,
            ecgAvailable = false,
            onMeasureEcgClick = {},
            autoSyncInterval = 0L,
            onAutoSyncIntervalChange = {},
            availableSensors = emptyMap(),
            onSensorToggle = { _, _ -> },
            showFlushDialog = false,
            onDismissFlushDialog = {},
            onConfirmFlush = {},
            showPermissionPermanentlyDeniedDialog = false,
            onDismissPermissionDialog = {},
            onOpenNotificationSettings = {},
            showEcgInstructionDialog = false,
            onDismissEcgInstructionDialog = {},
            onStartEcgMeasurement = {},
        )
    }
}

