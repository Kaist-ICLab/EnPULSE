package kaist.iclab.mobiletracker.ui.screens.HomeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        Spacer(modifier = Modifier.height(Styles.TOP_SPACER_HEIGHT))

        // 1. Greeting Section
        Column {
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
            isActive = uiState.isTrackingActive,
            lastSyncedTime = uiState.lastSyncedTime
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

        // 4. Highlight List - All sensors ordered alphabetically
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Styles.INSIGHT_ROW_VERTICAL_SPACING)
        ) {
            // Accelerometer (Watch)
            InsightRow(
                title = getSensorDisplayName("WatchAccelerometer"),
                value = stringResource(R.string.home_logs_unit, uiState.watchAccelerometerCount),
                icon = Icons.Default.Speed,
                iconColor = Styles.Colors.WATCH_ACCELEROMETER
            )
            // Ambient Light
            InsightRow(
                title = getSensorDisplayName("AmbientLight"),
                value = stringResource(R.string.home_logs_unit, uiState.ambientLightCount),
                icon = Icons.Default.LightMode,
                iconColor = Styles.Colors.AMBIENT_LIGHT
            )
            // App List Change
            InsightRow(
                title = getSensorDisplayName("AppListChange"),
                value = stringResource(R.string.home_logs_unit, uiState.appListChangeCount),
                icon = Icons.Default.AppRegistration,
                iconColor = Styles.Colors.APP_LIST_CHANGE
            )
            // App Usage
            InsightRow(
                title = getSensorDisplayName("AppUsage"),
                value = stringResource(R.string.home_logs_unit, uiState.appUsageCount),
                icon = Icons.Default.GridView,
                iconColor = Styles.Colors.APP_USAGE
            )
            // Battery Data
            InsightRow(
                title = getSensorDisplayName("Battery"),
                value = stringResource(R.string.home_logs_unit, uiState.batteryCount),
                icon = Icons.Default.BatteryChargingFull,
                iconColor = Styles.Colors.DEVICE_STATUS
            )
            // Bluetooth Scan
            InsightRow(
                title = getSensorDisplayName("BluetoothScan"),
                value = stringResource(R.string.home_logs_unit, uiState.bluetoothCount),
                icon = Icons.Default.Bluetooth,
                iconColor = Styles.Colors.BLUETOOTH
            )
            // Call Log
            InsightRow(
                title = getSensorDisplayName("CallLog"),
                value = stringResource(R.string.home_logs_unit, uiState.callLogCount),
                icon = Icons.Default.Call,
                iconColor = Styles.Colors.CALL_LOG
            )
            // Data Traffic
            InsightRow(
                title = getSensorDisplayName("DataTraffic"),
                value = stringResource(R.string.home_logs_unit, uiState.dataTrafficCount),
                icon = Icons.Default.DataUsage,
                iconColor = Styles.Colors.DATA_TRAFFIC
            )
            // Device Mode
            InsightRow(
                title = getSensorDisplayName("DeviceMode"),
                value = stringResource(R.string.home_logs_unit, uiState.deviceModeCount),
                icon = Icons.Default.SettingsSuggest,
                iconColor = Styles.Colors.DEVICE_MODE
            )
            // EDA (Watch)
            InsightRow(
                title = getSensorDisplayName("WatchEDA"),
                value = stringResource(R.string.home_logs_unit, uiState.watchEDACount),
                icon = Icons.Default.Waves,
                iconColor = Styles.Colors.WATCH_EDA
            )
            // Heart Rate (Watch)
            InsightRow(
                title = getSensorDisplayName("WatchHeartRate"),
                value = stringResource(R.string.home_logs_unit, uiState.watchHeartRateCount),
                icon = Icons.Default.FavoriteBorder,
                iconColor = Styles.Colors.WATCH_HEART_RATE
            )
            // Location Status
            InsightRow(
                title = getSensorDisplayName("Location"),
                value = stringResource(R.string.home_logs_unit, uiState.locationCount),
                icon = Icons.Default.Place,
                iconColor = Styles.Colors.LOCATION
            )
            // Media
            InsightRow(
                title = getSensorDisplayName("Media"),
                value = stringResource(R.string.home_logs_unit, uiState.mediaCount),
                icon = Icons.Default.PlayCircleOutline,
                iconColor = Styles.Colors.MEDIA
            )
            // Message
            InsightRow(
                title = getSensorDisplayName("MessageLog"),
                value = stringResource(R.string.home_logs_unit, uiState.messageLogCount),
                icon = Icons.AutoMirrored.Filled.Message,
                iconColor = Styles.Colors.MESSAGE_LOG
            )
            // Network Status
            InsightRow(
                title = getSensorDisplayName("Connectivity"),
                value = stringResource(R.string.home_logs_unit, uiState.connectivityCount),
                icon = Icons.Default.Wifi,
                iconColor = Styles.Colors.CONNECTIVITY
            )
            // Notification
            InsightRow(
                title = getSensorDisplayName("Notification"),
                value = stringResource(R.string.home_logs_unit, uiState.notificationCount),
                icon = Icons.Default.Notifications,
                iconColor = Styles.Colors.NOTIFICATIONS
            )
            // Physical Activity
            InsightRow(
                title = getSensorDisplayName("Step"),
                value = stringResource(R.string.home_logs_unit, uiState.activityCount),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconColor = Styles.Colors.ACTIVITY
            )
            // PPG (Watch)
            InsightRow(
                title = getSensorDisplayName("WatchPPG"),
                value = stringResource(R.string.home_logs_unit, uiState.watchPPGCount),
                icon = Icons.Default.MonitorHeart,
                iconColor = Styles.Colors.WATCH_PPG
            )
            // Screen Activity
            InsightRow(
                title = getSensorDisplayName("Screen"),
                value = stringResource(R.string.home_logs_unit, uiState.screenCount),
                icon = Icons.Default.StayCurrentPortrait,
                iconColor = Styles.Colors.SCREEN
            )
            // Skin Temperature (Watch)
            InsightRow(
                title = getSensorDisplayName("WatchSkinTemperature"),
                value = stringResource(R.string.home_logs_unit, uiState.watchSkinTemperatureCount),
                icon = Icons.Default.Thermostat,
                iconColor = Styles.Colors.WATCH_SKIN_TEMP
            )
            // User Interaction
            InsightRow(
                title = getSensorDisplayName("UserInteraction"),
                value = stringResource(R.string.home_logs_unit, uiState.userInteractionCount),
                icon = Icons.Default.TouchApp,
                iconColor = Styles.Colors.USER_INTERACTION
            )
            // Wifi Scan
            InsightRow(
                title = getSensorDisplayName("WifiScan"),
                value = stringResource(R.string.home_logs_unit, uiState.wifiScanCount),
                icon = Icons.Default.WifiTethering,
                iconColor = Styles.Colors.WIFI_SCAN
            )
        }

        Spacer(modifier = Modifier.height(Styles.BOTTOM_SPACER_HEIGHT))
    }
}
