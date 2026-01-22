package kaist.iclab.wearabletracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.ToggleChip
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.SensorNameText
import kaist.iclab.wearabletracker.ui.utils.getSensorDisplayName
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SensorToggleChip(
    sensorId: String,
    sensorStateFlow: StateFlow<SensorState>,
    updateStatus: (status: Boolean) -> Unit
) {
    val sensorState = sensorStateFlow.collectAsState().value
    val isEnabled =
        (sensorState.flag == SensorState.FLAG.ENABLED || sensorState.flag == SensorState.FLAG.RUNNING)
    val displayName = getSensorDisplayName(sensorId)

    ToggleChip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.sensorChipHorizontal,
                end = AppSpacing.sensorChipHorizontal,
                bottom = AppSpacing.sensorChipBottom
            )
            .height(AppSizes.sensorChipHeight),
        checked = isEnabled,
        toggleControl = {
            val switchOnText = stringResource(R.string.switch_on)
            val switchOffText = stringResource(R.string.switch_off)
            Switch(
                checked = isEnabled,
                modifier = Modifier.semantics {
                    this.contentDescription = if (isEnabled) switchOnText else switchOffText
                },
            )
        },
        onCheckedChange = updateStatus,
        label = {
            SensorNameText(
                text = displayName,
                maxLines = 1
            )
        }
    )
}
