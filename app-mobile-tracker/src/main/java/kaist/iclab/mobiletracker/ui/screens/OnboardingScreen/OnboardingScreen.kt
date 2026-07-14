package kaist.iclab.mobiletracker.ui.screens.OnboardingScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.viewmodels.onboarding.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onOnboardingComplete: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Navigate to home when onboarding is complete
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onOnboardingComplete()
        }
    }

    // Load campaigns on first composition
    LaunchedEffect(Unit) {
        viewModel.loadCampaigns()
    }

    // Show toast if there's an error (like survey fetch failure)
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when (error) {
                "onboarding_survey_fetch_error" -> context.getString(R.string.onboarding_survey_fetch_error)
                "turn_off_data_collection_first" -> context.getString(R.string.turn_off_data_collection_first)
                else -> error
            }
            AppToast.show(context, message)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = Styles.SCREEN_PADDING_HORIZONTAL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Section (Top Aligned)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Styles.HEADER_TOP_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo - smaller
            ImageAsset(
                assetPath = "icon.png",
                contentDescription = context.getString(R.string.mobile_tracker_logo),
                modifier = Modifier.size(Styles.LOGO_SIZE)
            )

            Spacer(modifier = Modifier.height(Styles.SPACING_L))

            // Welcome Title
            Text(
                text = context.getString(R.string.onboarding_welcome),
                fontSize = Styles.WELCOME_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Styles.SPACING_S))

            // Description
            Text(
                text = context.getString(R.string.onboarding_description),
                fontSize = Styles.DESCRIPTION_FONT_SIZE,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 2. Center Section (Fills remaining space and centers its content)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = RoundedCornerShape(Styles.CORNER_RADIUS),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.padding(Styles.SPACING_L)) {
                        Text(
                            text = context.getString(R.string.onboarding_loading),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = AppColors.TextSecondary
                        )
                    }
                } else if (uiState.campaigns.isEmpty()) {
                    Box(modifier = Modifier.padding(Styles.SPACING_L)) {
                        Text(
                            text = uiState.error
                                ?: context.getString(R.string.onboarding_no_campaigns),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = if (uiState.error != null) AppColors.ErrorColor else AppColors.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = Styles.LIST_MAX_HEIGHT)
                    ) {
                        itemsIndexed(uiState.campaigns) { index, campaign ->
                            CampaignListItem(
                                name = campaign.name,
                                isSelected = uiState.selectedCampaign?.id == campaign.id,
                                onClick = { viewModel.selectCampaign(campaign) },
                                showDivider = index < uiState.campaigns.lastIndex
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Section (Bottom Aligned)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Styles.BOTTOM_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = context.getString(R.string.onboarding_help_text),
                fontSize = Styles.HELP_FONT_SIZE,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Styles.SPACING_S)
            )

            Spacer(modifier = Modifier.height(Styles.SPACING_XXL))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        showPasswordDialog = true
                    },
                    modifier = Modifier
                        .weight(3f)
                        .height(Styles.BUTTON_HEIGHT),
                    enabled = uiState.selectedCampaign != null && !uiState.isLoading && !showPasswordDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.PrimaryColor,
                        contentColor = Color.White,
                        disabledContainerColor = AppColors.BorderLight,
                        disabledContentColor = AppColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(Styles.CORNER_RADIUS)
                ) {
                    Text(
                        text = context.getString(R.string.onboarding_start),
                        fontSize = Styles.START_BUTTON_FONT_SIZE,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(Styles.SPACING_M))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(Styles.BUTTON_HEIGHT),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ErrorColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(Styles.CORNER_RADIUS)
                ) {
                    Text(
                        text = context.getString(R.string.logout_title),
                        fontSize = Styles.LOGOUT_BUTTON_FONT_SIZE,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPasswordDialog && uiState.selectedCampaign != null) {
        kaist.iclab.mobiletracker.ui.components.CampaignDialog.PasswordDialog(
            campaignName = uiState.selectedCampaign!!.name,
            onDismiss = { showPasswordDialog = false },
            onVerify = { password ->
                viewModel.joinCampaign(password)
            },
            onSuccess = {
                showPasswordDialog = false
                viewModel.confirmSelection()
            }
        )
    }
}

/**
 * Reusable dropdown row component matching app style
 */
@Composable
fun CampaignListItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(
                    horizontal = Styles.ITEM_HORIZONTAL_PADDING,
                    vertical = Styles.ITEM_VERTICAL_PADDING
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = Styles.ITEM_FONT_SIZE,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = isSelected,
                onClick = null, // Handled by Row clickable
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.PrimaryColor,
                    unselectedColor = AppColors.TextSecondary
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = AppColors.BorderLight,
                thickness = Styles.BORDER_WIDTH,
                modifier = Modifier.padding(horizontal = Styles.ITEM_HORIZONTAL_PADDING)
            )
        }
    }
}
