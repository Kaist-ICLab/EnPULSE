package kaist.iclab.mobiletracker.ui.screens.SettingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.MainSettings.EnableTrackerCard
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.tracker.sensor.controller.ControllerState
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(title = context.getString(R.string.nav_settings))

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
                                if (settingsViewModel.hasNotificationPermission()) {
                                    settingsViewModel.startLogging()
                                } else {
                                    settingsViewModel.requestNotificationPermission()
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
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_account),
                            icon = Icons.Filled.AccountBox,
                            onClick = { navController.navigate(Screen.Account.route) },
                            iconTint = AppColors.IconAccount
                        )
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_server_sync),
                            icon = Icons.Filled.CloudSync,
                            onClick = { navController.navigate(Screen.ServerSync.route) },
                            iconTint = AppColors.IconSync
                        )
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_language),
                            icon = Icons.Filled.Language,
                            onClick = { navController.navigate(Screen.Language.route) },
                            iconTint = AppColors.IconLanguage
                        )
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_permission),
                            icon = Icons.Filled.Security,
                            onClick = { navController.navigate(Screen.Permission.route) },
                            iconTint = AppColors.IconSecurity
                        )
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_phone_sensor),
                            icon = Icons.Filled.PhoneAndroid,
                            onClick = { navController.navigate(Screen.PhoneSensor.route) },
                            iconTint = AppColors.IconPhone
                        )
                        SettingsMenuItemWithDivider(
                            title = context.getString(R.string.menu_about),
                            icon = Icons.Filled.Info,
                            onClick = { navController.navigate(Screen.About.route) },
                            showDivider = false,
                            iconTint = AppColors.IconInfo
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings screen header component
 */
@Composable
private fun SettingsHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Styles.HEADER_HEIGHT)
            .padding(start = Styles.HEADER_START_PADDING, end = Styles.HEADER_END_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = Styles.HEADER_FONT_SIZE
        )
    }
}
