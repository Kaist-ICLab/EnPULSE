package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.SurveySettings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.settings.SurveySettingsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Survey Settings screen where users can see and trigger surveys
 */
@Composable
fun SurveySettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: SurveySettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val config = viewModel.config.collectAsState(initial = null).value
    val surveyList = config?.survey?.keys?.toList() ?: emptyList()

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
                    text = context.getString(R.string.menu_survey),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Description text
            Text(
                text = context.getString(R.string.survey_screen_description),
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING,
                    vertical = Styles.CARD_VERTICAL_PADDING
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(surveyList) { surveyId ->
                    SurveyCard(
                        surveyId = surveyId,
                        onTrigger = { viewModel.triggerSurvey(surveyId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SurveyCard(
    surveyId: String,
    onTrigger: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        shape = Styles.CARD_SHAPE,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Survey ID: $surveyId",
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onTrigger,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryColor),
                shape = Styles.CARD_SHAPE
            ) {
                Text(text = "Trigger Now")
            }
        }
    }
}
