package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.DataSyncSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.SettingsMenuItemWithDivider
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.settings.DataSyncSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles as SettingsStyles

/**
 * Data & Sync Management screen
 */
@Composable
fun ServerSyncSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: DataSyncSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // Observe state from ViewModel
    val currentTime by viewModel.currentTime.collectAsState()
    val lastWatchData by viewModel.lastWatchData.collectAsState()
    val lastSuccessfulUpload by viewModel.lastSuccessfulUpload.collectAsState()
    val isFlushing by viewModel.isFlushing.collectAsState()

    // Dialog state
    var showFlushDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Styles.HEADER_HEIGHT)
                    .padding(horizontal = Styles.HEADER_HORIZONTAL_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = context.getString(R.string.menu_server_sync),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Main content - scrollable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        PaddingValues(
                            start = SettingsStyles.CARD_CONTAINER_HORIZONTAL_PADDING,
                            top = 8.dp,
                            end = SettingsStyles.CARD_CONTAINER_HORIZONTAL_PADDING,
                            bottom = SettingsStyles.CARD_VERTICAL_PADDING
                        )
                    )
            ) {
                // Timestamp information card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.White),
                    shape = SettingsStyles.CARD_SHAPE
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Styles.CARD_CONTENT_PADDING)
                    ) {
                        // Current Time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Styles.TEXT_SPACING),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.sync_current_time),
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTime,
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary
                            )
                        }

                        // Last Watch Data Received
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Styles.TEXT_SPACING),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.sync_last_watch_data),
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastWatchData ?: "--",
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary
                            )
                        }

                        // Last Successful Upload
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Styles.TEXT_SPACING),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.sync_last_successful_upload),
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastSuccessfulUpload ?: "--",
                                fontSize = Styles.TEXT_FONT_SIZE,
                                color = AppColors.TextPrimary
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(Styles.BUTTON_SPACING))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Styles.BUTTON_ROW_SPACING)
                ) {
                    // Upload all data button
                    Button(
                        onClick = {
                            viewModel.uploadAllSensorData()
                        },
                        modifier = Modifier
                            .weight(3f)
                            .height(Styles.BUTTON_HEIGHT),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryColor
                        ),
                        shape = RoundedCornerShape(Styles.BUTTON_CORNER_RADIUS)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Upload,
                                contentDescription = null,
                                tint = AppColors.White,
                                modifier = Modifier.size(Styles.BUTTON_ICON_SIZE)
                            )
                            Spacer(modifier = Modifier.width(Styles.BUTTON_ICON_SPACING))
                            Text(
                                text = context.getString(R.string.sync_start_data_upload),
                                color = AppColors.White,
                                fontSize = Styles.BUTTON_TEXT_SIZE
                            )
                        }
                    }

                    // Delete Data button
                    androidx.compose.material3.Button(
                        onClick = { showFlushDialog = true },
                        modifier = Modifier
                            .weight(2f)
                            .height(Styles.BUTTON_HEIGHT),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = AppColors.ErrorColor
                        ),
                        shape = RoundedCornerShape(Styles.BUTTON_CORNER_RADIUS),
                        enabled = !isFlushing
                    ) {
                        Text(
                            text = context.getString(R.string.sync_delete_data),
                            color = AppColors.White,
                            fontSize = Styles.BUTTON_TEXT_SIZE
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Styles.SECTION_TITLE_SPACING))

                // Settings Menu Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.White),
                    shape = SettingsStyles.CARD_SHAPE
                ) {
                    SettingsMenuItemWithDivider(
                        title = context.getString(R.string.sync_automatic_sync),
                        icon = Icons.Filled.Sync,
                        onClick = { navController.navigate(Screen.AutomaticSync.route) },
                        showDivider = false
                    )
                }
            }
        }
    }

    // Flush confirmation dialog
    if (showFlushDialog) {
        PopupDialog(
            title = context.getString(R.string.sync_clear_data_title),
            content = {
                Text(
                    text = context.getString(R.string.sync_clear_data_message),
                    fontSize = 14.sp,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = context.getString(R.string.sync_clear_data_confirm),
                onClick = {
                    viewModel.flushAllData()
                    showFlushDialog = false
                },
                enabled = !isFlushing
            ),
            secondaryButton = DialogButtonConfig(
                text = context.getString(R.string.sync_clear_data_cancel),
                onClick = { showFlushDialog = false },
                isPrimary = false
            ),
            onDismiss = { showFlushDialog = false }
        )
    }
}
