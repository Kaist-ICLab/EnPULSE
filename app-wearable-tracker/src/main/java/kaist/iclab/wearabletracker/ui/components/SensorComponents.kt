package kaist.iclab.wearabletracker.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.SensorNameText
import kaist.iclab.wearabletracker.ui.utils.formatCompactCount
import kaist.iclab.wearabletracker.ui.utils.getSensorDisplayName
import kotlinx.coroutines.flow.StateFlow

/**
 * @param pendingRecordCount records held for this sensor that the phone hasn't confirmed
 *   receiving yet. Shown next to the sensor name so the wearer can see which sensor is
 *   behind, not just the combined backlog in the status row. 0 renders nothing.
 */
@Composable
fun SensorToggleChip(
    sensorId: String,
    sensorStateFlow: StateFlow<SensorState>,
    isCollecting: Boolean,
    pendingRecordCount: Int = 0,
    updateStatus: (status: Boolean) -> Unit
) {
    val context = LocalContext.current
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
        onCheckedChange = { status ->
            if (isCollecting) {
                Toast.makeText(context, R.string.turn_off_data_collection_first, Toast.LENGTH_SHORT)
                    .show()
            } else {
                updateStatus(status)
            }
        },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SensorNameText(
                    text = displayName,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (pendingRecordCount > 0) {
                    val pendingText = formatCompactCount(pendingRecordCount)
                    val pendingDescription =
                        stringResource(R.string.pending_upload_description, pendingRecordCount)
                    Text(
                        text = stringResource(R.string.pending_upload_format, pendingText),
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = AppSpacing.sm)
                            .semantics { this.contentDescription = pendingDescription }
                    )
                }
            }
        }
    )
}
