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
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.onboarding.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // 1. Top section: Logo, title, desc
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo - smaller
            ImageAsset(
                assetPath = "icon.png",
                contentDescription = context.getString(R.string.mobile_tracker_logo),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Title
            Text(
                text = context.getString(R.string.onboarding_welcome),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = context.getString(R.string.onboarding_description),
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 2. Center section: Campaign List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(BiasAlignment(0f, 0.2f))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.onboarding_loading),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = AppColors.TextSecondary
                        )
                    }
                } else if (uiState.campaigns.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.onboarding_no_campaigns),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = context.getString(R.string.onboarding_help_text),
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        // 3. Bottom section: Start button
        Button(
            onClick = { viewModel.confirmSelection() },
            modifier = Modifier
                .fillMaxWidth()
                .height(Styles.BUTTON_HEIGHT)
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            enabled = uiState.selectedCampaign != null && !uiState.isLoading,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
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
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
