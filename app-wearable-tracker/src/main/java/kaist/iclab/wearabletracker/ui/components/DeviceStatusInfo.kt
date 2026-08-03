package kaist.iclab.wearabletracker.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.data.DeviceInfo
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.DeviceNameText
import kaist.iclab.wearabletracker.theme.SyncStatusText
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DeviceStatusInfo(
    deviceInfo: DeviceInfo,
    lastSyncTimestamp: Long?,
    totalRecordCount: Int = 0,
    batteryLevel: Int = -1,
    isRecording: Boolean = false,
    isPaused: Boolean = false,
    recordingStartTime: Long? = null,
    syncProgress: Float? = null,
    isPhoneConnected: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppSpacing.deviceInfoBottom,
                top = AppSpacing.deviceInfoTop
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device name with recording indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRecording) {
                PulsingDot(
                    modifier = Modifier.padding(end = 4.dp)
                )
            } else if (isPaused) {
                PulsingDot(
                    color = Color(0xFFFFA726), // Amber for paused
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            DeviceNameText(text = deviceInfo.name)
        }

        // Paused indicator
        if (isPaused) {
            Text(
                text = stringResource(R.string.paused_not_worn),
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                color = Color(0xFFFFA726), // Amber
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // Sync status: Show percentage if syncing, else show last sync time
        val syncText = when {
            syncProgress != null -> {
                val percentage = (syncProgress * 100).toInt()
                stringResource(R.string.syncing_progress_format, percentage)
            }

            lastSyncTimestamp != null -> {
                stringResource(R.string.last_sync_format, formatSyncTimestamp(lastSyncTimestamp))
            }

            else -> {
                stringResource(R.string.last_sync_placeholder)
            }
        }
        SyncStatusText(text = syncText)

        // Status row: battery | records | duration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery
            if (batteryLevel >= 0) {
                StatusChip(
                    text = stringResource(R.string.status_battery_format, batteryLevel),
                    color = when {
                        batteryLevel <= 15 -> Color(0xFFEF5350) // Red
                        batteryLevel <= 30 -> Color(0xFFFFA726) // Orange
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    }
                )
                StatusDivider()
            }

            // Record count
            StatusChip(
                text = stringResource(
                    R.string.status_records_format,
                    formatCount(totalRecordCount)
                ),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            // Recording duration
            if (isRecording && recordingStartTime != null) {
                StatusDivider()
                RecordingDuration(startTime = recordingStartTime)
            }

            // Phone connection
            StatusDivider()
            StatusChip(
                text = if (isPhoneConnected) "📱" else "📱✕",
                color = if (isPhoneConnected)
                    Color(0xFF66BB6A) // Green
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Pulsing red dot animation to indicate active recording.
 */
@Composable
private fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEF5350) // Red by default
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(6.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

/**
 * Small text chip for the status row.
 */
@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text = text,
        style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Normal),
        color = color,
        textAlign = TextAlign.Center
    )
}

/**
 * Divider dot between status items.
 */
@Composable
private fun StatusDivider() {
    Text(
        text = " · ",
        style = TextStyle(fontSize = 8.sp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
    )
}

/**
 * Live-updating recording duration display.
 */
@Composable
private fun RecordingDuration(startTime: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(startTime) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L.milliseconds)
        }
    }

    val elapsed = (now - startTime) / 1000
    val hours = elapsed / 3600
    val minutes = (elapsed % 3600) / 60
    val seconds = elapsed % 60

    val durationText = if (hours > 0) {
        String.format(LocalLocale.current.platformLocale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(LocalLocale.current.platformLocale, "%02d:%02d", minutes, seconds)
    }

    StatusChip(
        text = durationText,
        color = Color(0xFF66BB6A) // Green for active recording
    )
}

/**
 * Format large numbers compactly: 1234 -> "1.2K", 1234567 -> "1.2M"
 */
@SuppressLint("DefaultLocale")
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

/**
 * Format the sync timestamp to "YYYY/MM/DD HH.mm" format.
 */
private fun formatSyncTimestamp(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH.mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
