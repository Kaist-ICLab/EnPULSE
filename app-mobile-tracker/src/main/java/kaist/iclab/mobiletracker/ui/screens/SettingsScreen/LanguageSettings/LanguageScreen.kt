package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.LanguageSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Language settings screen
 */
@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageHelper = LanguageHelper(context)
    var selectedLanguage by remember { mutableStateOf(languageHelper.getLanguage()) }

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
                    text = context.getString(R.string.menu_language),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }
            
            // Description text
            Text(
                text = context.getString(R.string.language_screen_description),
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

            // Language list container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Styles.CONTAINER_HORIZONTAL_PADDING)
                    .padding(bottom = Styles.CONTAINER_BOTTOM_PADDING)
                    .clip(Styles.CONTAINER_SHAPE)
                    .background(AppColors.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val languages = listOf("en", "ko")
                    languages.forEachIndexed { index, languageCode ->
                        val isLast = index == languages.size - 1
                        val isSelected = selectedLanguage == languageCode

                        LanguageItem(
                            number = index + 1,
                            languageName = when (languageCode) {
                                "en" -> context.getString(R.string.language_english_full)
                                "ko" -> context.getString(R.string.language_korean_full)
                                else -> ""
                            },
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    languageHelper.saveLanguage(languageCode)
                                    selectedLanguage = languageCode
                                    onLanguageChanged()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!isLast) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
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
    }
}
