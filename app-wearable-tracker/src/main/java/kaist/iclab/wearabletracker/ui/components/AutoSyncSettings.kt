package kaist.iclab.wearabletracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.dialog.Dialog
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.AppTypography
import kaist.iclab.wearabletracker.theme.SensorNameText

/**
 * Single unified control for auto-sync: tapping it opens the interval picker, and picking
 * "Off" (0ms) is what turns auto-sync off — there's no separate enable/disable toggle.
 */
@Composable
fun AutoSyncSettings(
    intervalMs: Long,
    onIntervalChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showIntervalDialog by remember { mutableStateOf(false) }

    val intervals = listOf(
        0L to stringResource(R.string.interval_none),
        300_000L to stringResource(R.string.interval_5m),
        1_800_000L to stringResource(R.string.interval_30m),
        3_600_000L to stringResource(R.string.interval_1h),
        7_200_000L to stringResource(R.string.interval_2h),
        21_600_000L to stringResource(R.string.interval_6h),
        43_200_000L to stringResource(R.string.interval_12h)
    )

    val enabled = intervalMs > 0L
    val currentIntervalLabel = intervals.find { it.first == intervalMs }?.second
        ?: stringResource(R.string.interval_none)

    // Animated icon tint based on enabled state
    val syncIconColor by animateColorAsState(
        targetValue = if (enabled)
            MaterialTheme.colors.primary
        else
            MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
        label = "syncIconColor"
    )

    Chip(
        modifier = modifier
            .padding(
                bottom = AppSpacing.sensorChipBottom
            )
            .height(AppSizes.buttonMedium),
        onClick = { showIntervalDialog = true },
        label = {
            SensorNameText(
                text = currentIntervalLabel,
                maxLines = 1
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = syncIconColor,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = ChipDefaults.secondaryChipColors()
    )

    // ── Interval selection dialog — styled like a default Wear OS settings list ──
    Dialog(
        showDialog = showIntervalDialog,
        onDismissRequest = { showIntervalDialog = false }
    ) {
        val listState = rememberScalingLazyListState()
        Scaffold(
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(scalingLazyListState = listState)
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.auto_sync_label),
                            style = AppTypography.dialogTitle
                        )
                    }
                }
                items(intervals) { (ms, label) ->
                    val isSelected = ms == intervalMs
                    ToggleChip(
                        modifier = Modifier.fillMaxWidth(),
                        checked = isSelected,
                        onCheckedChange = {
                            onIntervalChange(ms)
                            showIntervalDialog = false
                        },
                        label = { Text(label) },
                        toggleControl = {
                            RadioButton(selected = isSelected)
                        }
                    )
                }
            }
        }
    }
}
