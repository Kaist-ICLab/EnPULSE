package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AboutSettings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * About settings screen
 * Displays app information
 */
@Composable
fun AboutSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current

    // Get app version
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
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
                    text = context.getString(R.string.menu_about),
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
                // App logo
                ImageAsset(
                    assetPath = "icon.png",
                    contentDescription = context.getString(R.string.mobile_tracker_logo),
                    modifier = Modifier.size(Styles.LOGO_SIZE)
                )
                
                Spacer(modifier = Modifier.height(Styles.LOGO_BOTTOM_PADDING))

                // App name
                Text(
                    text = context.getString(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.NAME_FONT_SIZE,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(Styles.DESCRIPTION_TOP_PADDING))

                // Description text
                Text(
                    text = context.getString(R.string.about_description),
                    fontSize = Styles.DESCRIPTION_FONT_SIZE,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(Styles.DESCRIPTION_WIDTH_RATIO)
                        .padding(horizontal = Styles.DESCRIPTION_HORIZONTAL_PADDING)
                )

                Spacer(modifier = Modifier.height(Styles.INFO_TOP_PADDING))

                // App information list
                Column(
                    modifier = Modifier
                        .fillMaxWidth(Styles.INFO_CARD_WIDTH_RATIO)
                        .padding(horizontal = Styles.INFO_CARD_PADDING)
                ) {
                    // Version row
                    InfoRow(
                        label = context.getString(R.string.version_label),
                        value = appVersion
                    )
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
        horizontalArrangement = Arrangement.SpaceBetween
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
