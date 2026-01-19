package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PhoneSensorConfigSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kaist.iclab.mobiletracker.ui.components.Toggle.Toggle
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    sensorName: String,
    sensorStateFlow: StateFlow<SensorState>,
    isControllerRunning: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = AppColors.Transparent),
        shape = Styles.CARD_SHAPE
    ) {
        SensorRow(
            sensorName = sensorName,
            sensorStateFlow = sensorStateFlow,
            isControllerRunning = isControllerRunning,
            toggleSensor = onToggle
        )
    }
}

@Composable
private fun SensorRow(
    sensorName: String,
    sensorStateFlow: StateFlow<SensorState>,
    isControllerRunning: Boolean,
    toggleSensor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensorState = sensorStateFlow.collectAsState().value
    val isSensorEnabled = sensorState.flag == SensorState.FLAG.RUNNING ||
            sensorState.flag == SensorState.FLAG.ENABLED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Styles.CARD_HORIZONTAL_PADDING,
                vertical = Styles.CARD_VERTICAL_PADDING
            )
    ) {
        Icon(
            imageVector = getSensorIcon(sensorName),
            contentDescription = sensorName,
            modifier = Modifier.size(Styles.ICON_SIZE),
            tint = AppColors.getSensorColor(sensorName)
        )
        Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = getLocalizedSensorTitle(sensorName),
                color = AppColors.TextPrimary,
                fontSize = Styles.TEXT_FONT_SIZE,
                lineHeight = Styles.TEXT_LINE_HEIGHT,
                modifier = Modifier.padding(top = Styles.TEXT_TOP_PADDING)
            )
            Text(
                text = getSensorDescription(sensorName),
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
        Toggle(
            checked = isSensorEnabled,
            onCheckedChange = { toggleSensor() },
            enabled = !isControllerRunning
        )
    }
}
