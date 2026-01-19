package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.DataSyncSettings.AutomaticSyncSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import org.koin.compose.koinInject

/**
 * Automatic Sync Settings screen
 */
@Composable
fun AutomaticSyncSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    syncTimestampService: SyncTimestampService = koinInject()
) {
    val context = LocalContext.current

    // Data collection running if started timestamp is not null
    val isDataCollectionRunning by remember {
        mutableStateOf(syncTimestampService.getDataCollectionStarted() != null)
    }

    // Automatic sync interval and network settings
    var selectedIntervalMs by remember {
        mutableStateOf(syncTimestampService.getAutoSyncIntervalMs())
    }
    var selectedNetworkMode by remember {
        mutableStateOf(syncTimestampService.getAutoSyncNetworkMode())
    }

    // Dialog states
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }

    // Settings items
    val settingsItems = listOf(
        SettingItem.Interval,
        SettingItem.Network
    )

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
                    text = context.getString(R.string.sync_automatic_sync),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Description text
            Text(
                text = context.getString(R.string.sync_automatic_sync_description),
                color = AppColors.TextSecondary,
                fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        end = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        bottom = Styles.SCREEN_DESCRIPTION_BOTTOM_PADDING
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING)
                    .padding(bottom = Styles.SETTING_CONTAINER_BOTTOM_PADDING)
                    .clip(Styles.CONTAINER_SHAPE)
                    .background(AppColors.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    settingsItems.forEachIndexed { index, item ->
                        val isLast = index == settingsItems.size - 1
                        val currentValue = when (item) {
                            SettingItem.Interval -> getIntervalLabel(context, selectedIntervalMs)
                            SettingItem.Network -> getNetworkLabel(context, selectedNetworkMode)
                        }
                        val icon = when (item) {
                            SettingItem.Interval -> Icons.Filled.Schedule
                            SettingItem.Network -> Icons.Filled.NetworkCheck
                        }
                        val title = when (item) {
                            SettingItem.Interval -> context.getString(R.string.sync_interval_title)
                            SettingItem.Network -> context.getString(R.string.sync_network_title)
                        }
                        val description = when (item) {
                            SettingItem.Interval -> context.getString(R.string.sync_interval_description)
                            SettingItem.Network -> context.getString(R.string.sync_network_description)
                        }

                        AutomaticSyncSettingCard(
                            title = title,
                            description = description,
                            currentValue = currentValue,
                            icon = icon,
                            isEnabled = !isDataCollectionRunning,
                            onClick = {
                                if (isDataCollectionRunning) {
                                    AppToast.show(context, R.string.turn_off_data_collection_first)
                                } else {
                                    when (item) {
                                        SettingItem.Interval -> showIntervalDialog = true
                                        SettingItem.Network -> showNetworkDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Add horizontal divider between cards (not after the last one)
                        if (!isLast) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalDivider(
                                    color = AppColors.BorderDark,
                                    thickness = 0.dp,
                                    modifier = Modifier.fillMaxWidth(Styles.DIVIDER_WIDTH_RATIO)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sync Interval Dialog
        if (showIntervalDialog) {
            IntervalSelectionDialog(
                selectedIntervalMs = selectedIntervalMs,
                onDismiss = { showIntervalDialog = false },
                onSelect = { intervalMs ->
                    selectedIntervalMs = intervalMs
                    syncTimestampService.setAutoSyncIntervalMs(intervalMs)
                    showIntervalDialog = false
                }
            )
        }

        // Sync Network Dialog
        if (showNetworkDialog) {
            NetworkSelectionDialog(
                selectedMode = selectedNetworkMode,
                onDismiss = { showNetworkDialog = false },
                onSelect = { mode ->
                    selectedNetworkMode = mode
                    syncTimestampService.setAutoSyncNetworkMode(mode)
                    showNetworkDialog = false
                }
            )
        }
    }
}

private sealed class SettingItem {
    abstract val name: String

    object Interval : SettingItem() {
        override val name = "Interval"
    }

    object Network : SettingItem() {
        override val name = "Network"
    }
}

@Composable
private fun AutomaticSyncSettingCard(
    title: String,
    description: String,
    currentValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Transparent),
        shape = Styles.CARD_SHAPE
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isEnabled) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = Styles.CARD_HORIZONTAL_PADDING,
                    vertical = Styles.CARD_VERTICAL_PADDING
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(Styles.ICON_SIZE),
                tint = AppColors.PrimaryColor
            )
            Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = Styles.TEXT_FONT_SIZE,
                    lineHeight = Styles.TEXT_LINE_HEIGHT,
                    modifier = Modifier.padding(top = Styles.TEXT_TOP_PADDING)
                )
                Text(
                    text = currentValue,
                    color = AppColors.PrimaryColor,
                    fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                    lineHeight = Styles.STATUS_TEXT_LINE_HEIGHT,
                    modifier = Modifier.padding(top = Styles.STATUS_TOP_PADDING)
                )
                Text(
                    text = description,
                    color = AppColors.TextSecondary,
                    fontSize = Styles.CARD_DESCRIPTION_FONT_SIZE,
                    lineHeight = Styles.CARD_DESCRIPTION_LINE_HEIGHT,
                    modifier = Modifier
                        .padding(
                            top = Styles.CARD_DESCRIPTION_TOP_PADDING,
                            bottom = Styles.CARD_DESCRIPTION_BOTTOM_PADDING
                        )
                )
            }
            if (isEnabled) {
                Spacer(Modifier.width(Styles.SPACER_WIDTH))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun IntervalSelectionDialog(
    selectedIntervalMs: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val context = LocalContext.current
    val options = listOf(
        SyncTimestampService.AUTO_SYNC_INTERVAL_NONE,
        SyncTimestampService.AUTO_SYNC_INTERVAL_30_SEC,
        SyncTimestampService.AUTO_SYNC_INTERVAL_1_MIN,
        SyncTimestampService.AUTO_SYNC_INTERVAL_15_MIN,
        SyncTimestampService.AUTO_SYNC_INTERVAL_30_MIN,
        SyncTimestampService.AUTO_SYNC_INTERVAL_60_MIN,
        SyncTimestampService.AUTO_SYNC_INTERVAL_120_MIN
    )
    var selected by remember { mutableStateOf(selectedIntervalMs) }

    PopupDialog(
        title = context.getString(R.string.sync_interval_title),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { intervalMs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selected == intervalMs),
                                onClick = { selected = intervalMs },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == intervalMs),
                            onClick = { selected = intervalMs },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.PrimaryColor
                            )
                        )
                        Text(
                            text = getIntervalLabel(context, intervalMs),
                            fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_select),
            onClick = {
                onSelect(selected)
                onDismiss()
            },
            enabled = true
        ),
        secondaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}

@Composable
private fun NetworkSelectionDialog(
    selectedMode: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val options = listOf(
        SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_MOBILE,
        SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_ONLY,
        SyncTimestampService.AUTO_SYNC_NETWORK_MOBILE_ONLY
    )
    var selected by remember { mutableStateOf(selectedMode) }

    PopupDialog(
        title = context.getString(R.string.sync_network_title),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selected == mode),
                                onClick = { selected = mode },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == mode),
                            onClick = { selected = mode },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.PrimaryColor
                            )
                        )
                        Text(
                            text = getNetworkLabel(context, mode),
                            fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_select),
            onClick = {
                onSelect(selected)
                onDismiss()
            },
            enabled = true
        ),
        secondaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}

private fun getIntervalLabel(context: android.content.Context, intervalMs: Long): String {
    return when (intervalMs) {
        SyncTimestampService.AUTO_SYNC_INTERVAL_NONE -> context.getString(R.string.sync_interval_option_none)
        SyncTimestampService.AUTO_SYNC_INTERVAL_15_MIN -> context.getString(R.string.sync_interval_option_15_min)
        SyncTimestampService.AUTO_SYNC_INTERVAL_30_MIN -> context.getString(R.string.sync_interval_option_30_min)
        SyncTimestampService.AUTO_SYNC_INTERVAL_60_MIN -> context.getString(R.string.sync_interval_option_60_min)
        SyncTimestampService.AUTO_SYNC_INTERVAL_120_MIN -> context.getString(R.string.sync_interval_option_120_min)
        SyncTimestampService.AUTO_SYNC_INTERVAL_30_SEC -> context.getString(R.string.sync_interval_option_30_sec)
        SyncTimestampService.AUTO_SYNC_INTERVAL_1_MIN -> context.getString(R.string.sync_interval_option_1_min)
        else -> context.getString(R.string.sync_interval_option_none)
    }
}

private fun getNetworkLabel(context: android.content.Context, mode: Int): String {
    return when (mode) {
        SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_MOBILE -> context.getString(R.string.sync_network_option_all)
        SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_ONLY -> context.getString(R.string.sync_network_option_wifi_only)
        SyncTimestampService.AUTO_SYNC_NETWORK_MOBILE_ONLY -> context.getString(R.string.sync_network_option_mobile_only)
        else -> context.getString(R.string.sync_network_option_all)
    }
}
