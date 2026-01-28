package kaist.iclab.mobiletracker.ui.screens.OnboardingScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.onboarding.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onOnboardingComplete: () -> Unit,
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Language state
    val languageHelper = LanguageHelper(context)
    var selectedLanguage by remember { mutableStateOf(languageHelper.getLanguage()) }
    
    // Dropdown expanded states
    var campaignExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

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

        // 2. Center section: Dropdowns - Absolutely centered on the page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Campaign Dropdown Row
                DropdownRow(
                    label = context.getString(R.string.onboarding_select_campaign),
                    value = uiState.selectedCampaign?.name
                        ?: if (uiState.isLoading) context.getString(R.string.onboarding_loading)
                        else if (uiState.campaigns.isEmpty()) context.getString(R.string.onboarding_no_campaigns)
                        else "-",
                    expanded = campaignExpanded,
                    onExpandChange = { campaignExpanded = it },
                    items = uiState.campaigns.map { it.name },
                    onItemSelected = { index ->
                        viewModel.selectCampaign(uiState.campaigns[index])
                        campaignExpanded = false
                    }
                )
                
                // Divider
                HorizontalDivider(
                    color = AppColors.BorderLight,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Language Dropdown Row
                DropdownRow(
                    label = context.getString(R.string.menu_language),
                    value = if (selectedLanguage == "ko") 
                        context.getString(R.string.language_korean)
                    else 
                        context.getString(R.string.language_english),
                    expanded = languageExpanded,
                    onExpandChange = { languageExpanded = it },
                    items = listOf(
                        context.getString(R.string.language_english),
                        context.getString(R.string.language_korean)
                    ),
                    onItemSelected = { index ->
                        val newLanguage = if (index == 0) "en" else "ko"
                        if (newLanguage != selectedLanguage) {
                            languageHelper.saveLanguage(newLanguage)
                            selectedLanguage = newLanguage
                            onLanguageChanged()
                        }
                        languageExpanded = false
                    }
                )
            }
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
private fun DropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    items: List<String>,
    onItemSelected: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded && items.isNotEmpty(),
            onDismissRequest = { onExpandChange(false) },
            containerColor = AppColors.White,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = item,
                            fontSize = 14.sp
                        ) 
                    },
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}
