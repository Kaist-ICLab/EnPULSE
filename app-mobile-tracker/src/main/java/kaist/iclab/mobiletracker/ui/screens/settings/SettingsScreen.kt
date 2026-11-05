package kaist.iclab.mobiletracker.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.components.AppHeader
import kaist.iclab.mobiletracker.ui.components.AppMenuItem
import kaist.iclab.mobiletracker.ui.components.DisabledSensorsWarningDialog
import kaist.iclab.mobiletracker.ui.components.FullScreenIntentPermissionDialog
import kaist.iclab.mobiletracker.ui.screens.settings.main.EnableTrackerCard
import kaist.iclab.mobiletracker.ui.screens.settings.sensor.getLocalizedSensorTitle
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsUiEvent
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import org.koin.androidx.compose.koinViewModel

/**
 * Settings screen with menu items
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val controllerState = settingsViewModel.controllerState.collectAsState().value
    val isCollecting = controllerState.flag == ControllerState.FLAG.RUNNING
    val sensorStateMap = settingsViewModel.sensorState.collectAsState().value

    var showFullScreenIntentWarning by remember { mutableStateOf(false) }
    var showDisabledSensorsWarning by remember { mutableStateOf(false) }
    var pendingDisabledSensorKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    // Collect one-shot UI events (toasts) from the ViewModel
    LaunchedEffect(Unit) {
        settingsViewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowToast ->
                    AppToast.show(context, event.messageResId)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(title = stringResource(R.string.nav_settings))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Styles.LAZY_COLUMN_TOP_PADDING,
                    bottom = Styles.CARD_VERTICAL_PADDING
                ),
                verticalArrangement = Arrangement.spacedBy(Styles.CARD_SPACING)
            ) {
                // Enable Tracker Card
                item {
                    EnableTrackerCard(
                        isCollecting = isCollecting,
                        isEnabled = true,
                        onToggle = { isChecked ->
                            if (isChecked) {
                                if (!NotificationHelper.canUseFullScreenIntent(context)) {
                                    showFullScreenIntentWarning = true
                                } else {
                                    val notEnabled = sensorStateMap.filter { (_, stateFlow) ->
                                        val flag = stateFlow.value.flag
                                        flag == SensorState.FLAG.DISABLED || flag == SensorState.FLAG.UNAVAILABLE
                                    }.keys.toList()

                                    if (notEnabled.isNotEmpty()) {
                                        pendingDisabledSensorKeys = notEnabled
                                        showDisabledSensorsWarning = true
                                    } else {
                                        proceedToNotificationCheck(context, settingsViewModel)
                                    }
                                }
                            } else {
                                settingsViewModel.stopLogging()
                            }
                        }
                    )
                }


                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING),
                        colors = CardDefaults.cardColors(containerColor = AppColors.White),
                        shape = Styles.CARD_SHAPE
                    ) {
                        AppMenuItem(
                            title = stringResource(R.string.menu_account),
                            icon = Icons.Filled.AccountBox,
                            onClick = { navController.navigate(Screen.Account.route) },
                            iconTint = AppColors.IconAccount
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_server_sync),
                            icon = Icons.Filled.CloudSync,
                            onClick = { navController.navigate(Screen.ServerSync.route) },
                            iconTint = AppColors.IconSync
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_server_connection),
                            icon = Icons.Filled.Storage,
                            onClick = { navController.navigate(Screen.ServerConnection.route) },
                            iconTint = AppColors.IconSync
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_language),
                            icon = Icons.Filled.Language,
                            onClick = { navController.navigate(Screen.Language.route) },
                            iconTint = AppColors.IconLanguage
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_permission),
                            icon = Icons.Filled.Security,
                            onClick = { navController.navigate(Screen.Permission.route) },
                            iconTint = AppColors.IconSecurity
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_phone_sensor),
                            icon = Icons.Filled.PhoneAndroid,
                            onClick = { navController.navigate(Screen.PhoneSensor.route) },
                            iconTint = AppColors.IconPhone
                        )
                        AppMenuItem(
                            title = stringResource(R.string.menu_about),
                            icon = Icons.Filled.Info,
                            onClick = { navController.navigate(Screen.About.route) },
                            showDivider = false,
                            iconTint = AppColors.IconInfo
                        )
                    }
                }
            }
        }

        FullScreenIntentPermissionDialog(
            showDialog = showFullScreenIntentWarning,
            onDismiss = { showFullScreenIntentWarning = false },
            onConfirm = {
                showFullScreenIntentWarning = false
                NotificationHelper.openFullScreenIntentSettings(context)
            }
        )

        DisabledSensorsWarningDialog(
            showDialog = showDisabledSensorsWarning,
            sensorNames = pendingDisabledSensorKeys.map { getLocalizedSensorTitle(it) },
            onDismiss = { showDisabledSensorsWarning = false },
            onConfirm = {
                showDisabledSensorsWarning = false
                proceedToNotificationCheck(context, settingsViewModel)
            }
        )
    }
}

/**
 * Checks whether the POST_NOTIFICATIONS permission is granted.
 * On API levels below 33 this permission does not gate notifications, so the check returns granted.
 */
private fun hasNotificationPermission(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Requests the notification permission if needed, then starts logging. This is the final
 * step of the "Enable Tracker" toggle flow, reached either directly (no disabled sensors) or
 * after the user confirms "Start Anyway" on the disabled-sensors warning dialog.
 */
private fun proceedToNotificationCheck(context: Context, settingsViewModel: SettingsViewModel) {
    if (hasNotificationPermission(context)) {
        settingsViewModel.startLogging()
    } else {
        settingsViewModel.requestNotificationPermission()
    }
}


