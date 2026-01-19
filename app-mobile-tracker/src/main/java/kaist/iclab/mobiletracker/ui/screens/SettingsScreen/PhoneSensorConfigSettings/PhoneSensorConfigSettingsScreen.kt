package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PhoneSensorConfigSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.tracker.sensor.controller.ControllerState
import org.koin.androidx.compose.koinViewModel

/**
 * Phone Sensor screen with sensor management functionality
 */
@Composable
fun PhoneSensorConfigSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val controllerState = viewModel.controllerState.collectAsState().value
    val isCollecting = controllerState.flag == ControllerState.FLAG.RUNNING
    val sensorList = viewModel.sensorState.toList()

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
                    text = context.getString(R.string.menu_phone_sensor),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Description text
            Text(
                text = context.getString(R.string.phone_sensor_screen_description),
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
                    .fillMaxSize()
                    .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING)
                    .padding(bottom = Styles.SETTING_CONTAINER_BOTTOM_PADDING)
                    .clip(Styles.CONTAINER_SHAPE)
                    .background(AppColors.White)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = sensorList,
                        key = { _, pair -> pair.first }
                    ) { index, (sensorName, sensorStateFlow) ->
                        val isLast = index == sensorList.size - 1

                        SensorCard(
                            sensorName = sensorName,
                            sensorStateFlow = sensorStateFlow,
                            isControllerRunning = isCollecting,
                            onToggle = {
                                if (isCollecting) {
                                    AppToast.show(context, R.string.turn_off_data_collection_first)
                                } else {
                                    viewModel.toggleSensor(sensorName)
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
    }
}

