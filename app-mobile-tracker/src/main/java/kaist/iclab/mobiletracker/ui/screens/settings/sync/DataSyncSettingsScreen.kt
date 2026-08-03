package kaist.iclab.mobiletracker.ui.screens.settings.sync

import android.content.res.Resources
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import org.koin.compose.koinInject

/**
 * Auto Sync Settings screen (formerly Data & Sync)
 */
@Composable
fun ServerSyncSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    syncTimestampService: SyncTimestampService = koinInject()
) {
    // Automatic sync interval and network settings
    var selectedIntervalMs by remember {
        mutableLongStateOf(syncTimestampService.getAutoSyncIntervalMs())
    }
    var selectedNetworkMode by remember {
        mutableIntStateOf(syncTimestampService.getAutoSyncNetworkMode())
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
                    text = stringResource(R.string.menu_server_sync),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Description text
            Text(
                text = stringResource(R.string.sync_automatic_sync_description),
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
                            SettingItem.Interval -> getIntervalLabel(LocalResources.current, selectedIntervalMs)
                            SettingItem.Network -> getNetworkLabel(LocalResources.current, selectedNetworkMode)
                        }
                        val icon = when (item) {
                            SettingItem.Interval -> Icons.Filled.Schedule
                            SettingItem.Network -> Icons.Filled.NetworkCheck
                        }
                        val title = when (item) {
                            SettingItem.Interval -> stringResource(R.string.sync_interval_title)
                            SettingItem.Network -> stringResource(R.string.sync_network_title)
                        }
                        val description = when (item) {
                            SettingItem.Interval -> stringResource(R.string.sync_interval_description)
                            SettingItem.Network -> stringResource(R.string.sync_network_description)
                        }

                        AutomaticSyncSettingCard(
                            title = title,
                            description = description,
                            currentValue = currentValue,
                            icon = icon,
                            isEnabled = true,
                            onClick = {
                                when (item) {
                                    SettingItem.Interval -> showIntervalDialog = true
                                    SettingItem.Network -> showNetworkDialog = true
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
                .clickable { onClick() }
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

@Composable
private fun IntervalSelectionDialog(
    selectedIntervalMs: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val options = listOf(
        Constants.AutoSync.INTERVAL_NONE,
        Constants.AutoSync.INTERVAL_5_MIN,
        Constants.AutoSync.INTERVAL_30_MIN,
        Constants.AutoSync.INTERVAL_60_MIN,
        Constants.AutoSync.INTERVAL_2_HOUR,
        Constants.AutoSync.INTERVAL_6_HOUR,
        Constants.AutoSync.INTERVAL_12_HOUR
    )
    var selected by remember { mutableLongStateOf(selectedIntervalMs) }

    PopupDialog(
        title = stringResource(R.string.sync_interval_title),
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
                            text = getIntervalLabel(LocalResources.current, intervalMs),
                            fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = stringResource(R.string.campaign_dialog_select),
            onClick = {
                onSelect(selected)
                onDismiss()
            },
            enabled = true
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.campaign_dialog_cancel),
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
    val options = listOf(
        Constants.AutoSync.NETWORK_WIFI_MOBILE,
        Constants.AutoSync.NETWORK_WIFI_ONLY,
        Constants.AutoSync.NETWORK_MOBILE_ONLY
    )
    var selected by remember { mutableIntStateOf(selectedMode) }

    PopupDialog(
        title = stringResource(R.string.sync_network_title),
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
                            text = getNetworkLabel(LocalResources.current, mode),
                            fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = stringResource(R.string.campaign_dialog_select),
            onClick = {
                onSelect(selected)
                onDismiss()
            },
            enabled = true
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.campaign_dialog_cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}

private fun getIntervalLabel(resources: Resources, intervalMs: Long): String {
    return when (intervalMs) {
        Constants.AutoSync.INTERVAL_NONE -> resources.getString(R.string.sync_interval_option_none)
        Constants.AutoSync.INTERVAL_5_MIN -> resources.getString(R.string.sync_interval_option_5_min)
        Constants.AutoSync.INTERVAL_30_MIN -> resources.getString(R.string.sync_interval_option_30_min)
        Constants.AutoSync.INTERVAL_60_MIN -> resources.getString(R.string.sync_interval_option_60_min)
        Constants.AutoSync.INTERVAL_2_HOUR -> resources.getString(R.string.sync_interval_option_2_hour)
        Constants.AutoSync.INTERVAL_6_HOUR -> resources.getString(R.string.sync_interval_option_6_hour)
        Constants.AutoSync.INTERVAL_12_HOUR -> resources.getString(R.string.sync_interval_option_12_hour)
        else -> resources.getString(R.string.sync_interval_option_none)
    }
}

private fun getNetworkLabel(resources: Resources, mode: Int): String {
    return when (mode) {
        Constants.AutoSync.NETWORK_WIFI_MOBILE -> resources.getString(R.string.sync_network_option_all)
        Constants.AutoSync.NETWORK_WIFI_ONLY -> resources.getString(R.string.sync_network_option_wifi_only)
        Constants.AutoSync.NETWORK_MOBILE_ONLY -> resources.getString(R.string.sync_network_option_mobile_only)
        else -> resources.getString(R.string.sync_network_option_all)
    }
}
