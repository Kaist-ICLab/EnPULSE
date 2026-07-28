package kaist.iclab.mobiletracker.ui.screens.HomeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.utils.getSensorDisplayName
import kaist.iclab.mobiletracker.viewmodels.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(Styles.SCREEN_VERTICAL_SPACING)
    ) {
        // 1. Greeting Section
        Column(modifier = Modifier.padding(top = Styles.TOP_SPACER_HEIGHT)) {
            Text(
                text = stringResource(R.string.home_hello, uiState.userName ?: ""),
                fontSize = Styles.GREETING_TITLE_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                fontSize = Styles.GREETING_SUBTITLE_FONT_SIZE,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Styles.GREETING_SUBTITLE_TOP_PADDING)
            )
        }

        // 2. Tracking Status Card (upper card)
        TrackingStatusCard(
            isActive = uiState.isTrackingActive
        )

        // 3. Galaxy Watch Card (lower card)
        GalaxyWatchCard(
            watchStatus = uiState.watchStatus,
            connectedDevices = uiState.connectedDevices
        )

        // 3. Grid Section Title
        Text(
            text = stringResource(R.string.home_daily_highlights),
            fontSize = Styles.GRID_SECTION_TITLE_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(top = Styles.GRID_SECTION_TITLE_TOP_PADDING)
        )

        // 4. Highlight List - Only show sensors with data collected today
        // Create sensor items list with current counts, filtered to non-zero
        val sensorItems = remember(uiState) {
            listOf(
                SensorItem(
                    "Accelerometer",
                    uiState.watchAccelerometerCount,
                    Icons.Default.Speed,
                    Styles.Colors.WATCH_ACCELEROMETER
                ),
                SensorItem(
                    "AmbientLight",
                    uiState.ambientLightCount,
                    Icons.Default.LightMode,
                    Styles.Colors.AMBIENT_LIGHT
                ),
                SensorItem(
                    "AppListChange",
                    uiState.appListChangeCount,
                    Icons.Default.AppRegistration,
                    Styles.Colors.APP_LIST_CHANGE
                ),
                SensorItem(
                    "AppUsageLog",
                    uiState.appUsageCount,
                    Icons.Default.GridView,
                    Styles.Colors.APP_USAGE
                ),
                SensorItem(
                    "Battery",
                    uiState.batteryCount,
                    Icons.Default.BatteryChargingFull,
                    Styles.Colors.DEVICE_STATUS
                ),
                SensorItem(
                    "BluetoothScan",
                    uiState.bluetoothCount,
                    Icons.Default.Bluetooth,
                    Styles.Colors.BLUETOOTH
                ),
                SensorItem(
                    "CallLog",
                    uiState.callLogCount,
                    Icons.Default.Call,
                    Styles.Colors.CALL_LOG
                ),
                SensorItem(
                    "DataTraffic",
                    uiState.dataTrafficCount,
                    Icons.Default.DataUsage,
                    Styles.Colors.DATA_TRAFFIC
                ),
                SensorItem(
                    "DeviceMode",
                    uiState.deviceModeCount,
                    Icons.Default.SettingsSuggest,
                    Styles.Colors.DEVICE_MODE
                ),
                SensorItem(
                    "EDA",
                    uiState.watchEDACount,
                    Icons.Default.Waves,
                    Styles.Colors.WATCH_EDA
                ),
                SensorItem(
                    "HeartRate",
                    uiState.watchHeartRateCount,
                    Icons.Default.FavoriteBorder,
                    Styles.Colors.WATCH_HEART_RATE
                ),
                SensorItem(
                    "Location",
                    uiState.locationCount,
                    Icons.Default.Place,
                    Styles.Colors.LOCATION
                ),
                SensorItem(
                    "Media",
                    uiState.mediaCount,
                    Icons.Default.PlayCircleOutline,
                    Styles.Colors.MEDIA
                ),
                SensorItem(
                    "MessageLog",
                    uiState.messageLogCount,
                    Icons.AutoMirrored.Filled.Message,
                    Styles.Colors.MESSAGE_LOG
                ),
                SensorItem(
                    "Connectivity",
                    uiState.connectivityCount,
                    Icons.Default.Wifi,
                    Styles.Colors.CONNECTIVITY
                ),
                SensorItem(
                    "Notification",
                    uiState.notificationCount,
                    Icons.Default.Notifications,
                    Styles.Colors.NOTIFICATIONS
                ),
                SensorItem(
                    "Step",
                    uiState.activityCount,
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    Styles.Colors.ACTIVITY
                ),
                SensorItem(
                    "PPG",
                    uiState.watchPPGCount,
                    Icons.Default.MonitorHeart,
                    Styles.Colors.WATCH_PPG
                ),
                SensorItem(
                    "Screen",
                    uiState.screenCount,
                    Icons.Default.StayCurrentPortrait,
                    Styles.Colors.SCREEN
                ),
                SensorItem(
                    "SkinTemperature",
                    uiState.watchSkinTemperatureCount,
                    Icons.Default.Thermostat,
                    Styles.Colors.WATCH_SKIN_TEMP
                ),
                SensorItem(
                    "UserInteraction",
                    uiState.userInteractionCount,
                    Icons.Default.TouchApp,
                    Styles.Colors.USER_INTERACTION
                ),
                SensorItem(
                    "WifiScan",
                    uiState.wifiScanCount,
                    Icons.Default.WifiTethering,
                    Styles.Colors.WIFI_SCAN
                ),
                SensorItem(
                    "WatchIMU",
                    uiState.watchIMUCount,
                    Icons.Default.CompassCalibration,
                    Styles.Colors.WATCH_IMU
                ),
                SensorItem(
                    "WatchGesture",
                    uiState.watchGestureCount,
                    Icons.Default.BackHand,
                    Styles.Colors.WATCH_GESTURE
                ),
                SensorItem(
                    "WatchStress",
                    uiState.watchStressCount,
                    Icons.Default.Psychology,
                    Styles.Colors.WATCH_STRESS
                ),
                SensorItem(
                    "Exercise",
                    uiState.exerciseCount,
                    Icons.Default.FitnessCenter,
                    Styles.Colors.EXERCISE
                ),
                SensorItem(
                    "Sleep",
                    uiState.sleepCount,
                    Icons.Default.Bedtime,
                    Styles.Colors.SLEEP
                ),
                SensorItem(
                    "ECG",
                    uiState.ecgCount,
                    Icons.Default.Favorite,
                    Styles.Colors.ECG
                )
            ).filter { it.count > 0 }
        }

        if (sensorItems.isEmpty()) {
            // Empty state - no data collected today
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.PrimaryColor
                    )
                    Text(
                        text = stringResource(R.string.home_empty_state_message),
                        fontSize = Styles.GREETING_SUBTITLE_FONT_SIZE,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Styles.INSIGHT_ROW_VERTICAL_SPACING),
                contentPadding = PaddingValues(bottom = Styles.BOTTOM_SPACER_HEIGHT)
            ) {
                items(
                    items = sensorItems,
                    key = { it.sensorId }
                ) { sensor ->
                    InsightRow(
                        title = getSensorDisplayName(sensor.sensorId),
                        value = stringResource(R.string.home_logs_unit, sensor.count),
                        icon = sensor.icon,
                        iconColor = sensor.iconColor
                    )
                }
            }
        }
    }
}

/**
 * Data class representing a sensor item for the LazyColumn
 */
private data class SensorItem(
    val sensorId: String,
    val count: Int,
    val icon: ImageVector,
    val iconColor: Color
)
