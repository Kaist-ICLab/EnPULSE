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
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.viewmodels.settings.SurveySettingsViewModel
import org.koin.androidx.compose.koinViewModel

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
                    text = "Survey Configuration",
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(surveyList) { surveyId ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Survey ID: $surveyId",
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.triggerSurvey(surveyId) },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryColor)
                            ) {
                                Text("Trigger Now")
                            }
                        }
                    }
                }
            }
        }
    }
}
