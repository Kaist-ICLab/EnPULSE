package kaist.iclab.wearabletracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.AppTypography
import kaist.iclab.wearabletracker.theme.SensorNameText

@Composable
fun SettingController(
    startLogging: () -> Unit,
    stopLogging: () -> Unit,
    isCollecting: Boolean,
    hasEnabledSensors: Boolean,
    autoSyncIntervalMs: Long,
    onAutoSyncIntervalChange: (Long) -> Unit
) {
    // Start button requires at least one sensor enabled
    // Connection check is done in startLogging callback
    val canStartCollection = hasEnabledSensors

    Row(
        modifier = Modifier
            .padding(
                horizontal = AppSpacing.sensorChipHorizontal,
                vertical = AppSpacing.sensorChipBottom
            )
            .fillMaxWidth(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Chip(
            modifier = Modifier
                .weight(1f)
                .padding(
                    bottom = AppSpacing.sensorChipBottom
                )
                .height(AppSizes.buttonMedium),
            onClick = {
                if (isCollecting) {
                    stopLogging()
                } else if (canStartCollection) {
                    startLogging()
                }
                // Do nothing when disabled
            },
            label = {
                SensorNameText(
                    text = when {
                        isCollecting -> stringResource(R.string.stop_collection)
                        canStartCollection -> stringResource(R.string.start_collection)
                        else -> stringResource(R.string.start_collection)
                    },
                    color = MaterialTheme.colors.onPrimary
                )
            },
            icon = {
                Icon(
                    imageVector = if (isCollecting) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = when {
                isCollecting -> ChipDefaults.chipColors(MaterialTheme.colors.error)
                canStartCollection -> ChipDefaults.chipColors(MaterialTheme.colors.primary)
                else -> ChipDefaults.chipColors(MaterialTheme.colors.onSurface.copy(alpha = 0.3f)) // Greyed out
            },
        )

        // Fills the remaining space next to the start/stop button
        AutoSyncSettings(
            intervalMs = autoSyncIntervalMs,
            onIntervalChange = onAutoSyncIntervalChange,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Manual data-management actions (upload / delete all). Placed at the bottom of the sensor
 * list since they're used far less often than starting/stopping collection.
 */
@Composable
fun DataActionsRow(
    upload: () -> Unit,
    flush: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(horizontal = AppSpacing.sensorChipHorizontal * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DangerZoneIndicator()

        Button(
            onClick = upload,
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier.fillMaxWidth().height(AppSizes.buttonMedium)
        ) {
            Text(
                text = stringResource(R.string.upload_data),
                color = MaterialTheme.colors.onBackground
            )
        }

        Button(
            onClick = flush,
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier.fillMaxWidth().height(AppSizes.buttonMedium)
        ) {
            Text(
                text = stringResource(R.string.flush_data),
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

/**
 * Small warning label marking the destructive action(s) below it as irreversible.
 */
@Composable
private fun DangerZoneIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colors.error,
            modifier = Modifier.size(AppSizes.iconSmall)
        )
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        Text(
            text = stringResource(R.string.danger_zone_label),
            style = AppTypography.syncStatus,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.error
        )
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    backgroundColor: Color,
    buttonSize: Dp = 32.dp,
    iconSize: Dp = 20.dp,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
        modifier = Modifier
            .padding(AppSpacing.iconButtonPadding)
            .size(buttonSize)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}
