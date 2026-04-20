package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AccountSettings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.ui.components.CampaignDialog.CampaignDialog
import kaist.iclab.mobiletracker.ui.components.LogoutDialog.LogoutDialog
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.viewmodels.auth.AuthViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.AccountSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles as SettingsStyles

/**
 * Account settings screen
 * Displays user information similar to About phone screen
 */
@Composable
fun AccountSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = koinViewModel(),
    accountSettingsViewModel: AccountSettingsViewModel = koinViewModel(),
    syncTimestampService: SyncTimestampService = koinInject()
) {
    val context = LocalContext.current
    val userState by authViewModel.userState.collectAsState()

    // Campaign state from ViewModel
    val campaigns by accountSettingsViewModel.campaigns.collectAsState()
    val isLoadingCampaigns by accountSettingsViewModel.isLoadingCampaigns.collectAsState()
    val campaignError by accountSettingsViewModel.campaignError.collectAsState()
    val selectedCampaignId by accountSettingsViewModel.selectedCampaignId.collectAsState()
    val selectedCampaignName by accountSettingsViewModel.selectedCampaignName.collectAsState()

    // Data collection running state from ViewModel
    val isDataCollectionRunning by accountSettingsViewModel.isDataCollectionRunning.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCampaignDialog by remember { mutableStateOf(false) }
    var showReloadDialog by remember { mutableStateOf(false) }

    val isReloadingConfig by accountSettingsViewModel.isReloadingConfig.collectAsState()

    // Fetch campaigns when dialog is shown (refresh in case new campaigns were added)
    // Note: Initial data (campaigns + user campaign) is already loaded in ViewModel init
    LaunchedEffect(showCampaignDialog) {
        if (showCampaignDialog) {
            accountSettingsViewModel.fetchCampaigns()
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                authViewModel.logout()
                showLogoutDialog = false
            }
        )
    }

    // Campaign selection dialog
    if (showCampaignDialog) {
        CampaignDialog(
            campaigns = campaigns,
            selectedCampaignId = selectedCampaignId,
            isLoading = isLoadingCampaigns,
            error = campaignError,
            onDismiss = { showCampaignDialog = false },
            onSelect = { campaignId ->
                accountSettingsViewModel.selectCampaign(campaignId)
            },
            onJoinCampaign = { campaignId, password ->
                accountSettingsViewModel.joinCampaign(campaignId, password)
            }
        )
    }

    // Reload Config Dialog
    if (showReloadDialog) {
        PopupDialog(
            title = context.getString(R.string.reload_config_dialog_title),
            content = {
                Text(
                    text = context.getString(R.string.reload_config_dialog_message),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
            },
            primaryButton = DialogButtonConfig(
                text = context.getString(R.string.logout_confirm),
                onClick = {
                    accountSettingsViewModel.reloadConfig()
                    showReloadDialog = false
                },
            ),
            secondaryButton = DialogButtonConfig(
                text = context.getString(R.string.logout_close),
                onClick = { showReloadDialog = false },
                isPrimary = false
            ),
            onDismiss = {
                if (!isReloadingConfig) showReloadDialog = false
            }
        )
    }

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
                    text = context.getString(R.string.menu_account),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Styles.CONTENT_TOP_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User name
                Text(
                    text = userState.user?.name ?: context.getString(R.string.unknown),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.NAME_FONT_SIZE,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(Styles.BUTTON_TOP_PADDING))

                // Sign out button (replaces Rename button)
                Button(
                    onClick = {
                        // If user skipped login (not logged in and no user), just navigate to login
                        if (!userState.isLoggedIn && userState.user == null) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            // Otherwise show logout dialog
                            showLogoutDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(Styles.BUTTON_WIDTH_RATIO),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.BorderLight,
                        contentColor = AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(Styles.BUTTON_CORNER_RADIUS)
                ) {
                    Text(
                        text = context.getString(R.string.sign_out),
                        fontSize = Styles.BUTTON_TEXT_FONT_SIZE
                    )
                }

                Spacer(modifier = Modifier.height(Styles.INFO_TOP_PADDING))

                // User information list
                Column(
                    modifier = Modifier
                        .fillMaxWidth(Styles.INFO_CARD_WIDTH_RATIO)
                        .padding(horizontal = Styles.INFO_CARD_PADDING)
                ) {
                    // Email row
                    InfoRow(
                        label = context.getString(R.string.email_label),
                        value = userState.user?.email ?: context.getString(R.string.no_email)
                    )
                }

                Spacer(modifier = Modifier.height(Styles.INFO_TOP_PADDING))

                // Campaign menu card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsStyles.CARD_CONTAINER_HORIZONTAL_PADDING),
                    colors = CardDefaults.cardColors(containerColor = AppColors.White),
                    shape = SettingsStyles.CARD_SHAPE
                ) {
                    CampaignMenuItem(
                        title = context.getString(R.string.menu_campaign),
                        description = selectedCampaignName
                            ?: context.getString(R.string.campaign_no_campaign_joined),
                        hasSelectedExperiment = selectedCampaignName != null,
                        isEnabled = !isDataCollectionRunning,
                        onClick = {
                            if (isDataCollectionRunning) {
                                AppToast.show(context, R.string.turn_off_data_collection_first)
                            } else {
                                showCampaignDialog = true
                            }
                        }
                    )

                    // Reload Config Menu Item
                    if (selectedCampaignName != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = AppColors.BorderLight
                        )
                        CampaignMenuItem(
                            title = context.getString(R.string.menu_reload_config),
                            description = context.getString(R.string.menu_reload_config_desc),
                            icon = Icons.Filled.Refresh,
                            hasSelectedExperiment = false,
                            isEnabled = !isDataCollectionRunning && !isReloadingConfig,
                            onClick = {
                                if (isDataCollectionRunning) {
                                    AppToast.show(context, R.string.turn_off_data_collection_first)
                                } else {
                                    showReloadDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Styles.INFO_ROW_VERTICAL_PADDING),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = Styles.INFO_LABEL_FONT_SIZE,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = Styles.INFO_VALUE_FONT_SIZE,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun CampaignMenuItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.Assignment,
    hasSelectedExperiment: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1f else 0.6f)
            .clickable { onClick() }
            .padding(
                horizontal = SettingsStyles.MENU_ITEM_HORIZONTAL_PADDING,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.PrimaryColor,
            modifier = Modifier.size(SettingsStyles.ICON_SIZE)
        )
        Spacer(Modifier.width(SettingsStyles.ICON_SPACER_WIDTH))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = SettingsStyles.TEXT_FONT_SIZE,
                lineHeight = SettingsStyles.TEXT_LINE_HEIGHT,
                modifier = Modifier.padding(top = SettingsStyles.TEXT_TOP_PADDING)
            )
            Text(
                text = description,
                color = if (hasSelectedExperiment) AppColors.PrimaryColor else AppColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = 3.dp
                )
            )
        }
        if (isEnabled) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextSecondary
            )
        }
    }
}
