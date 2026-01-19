package kaist.iclab.wearabletracker.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Reusable style composables
 * These are pre-styled composables that can be used throughout the app
 */

/**
 * Styled text for device name
 */
@Composable
fun DeviceNameText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.deviceName,
    color: Color = MaterialTheme.colors.onSurface
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/**
 * Styled text for sync status
 */
@Composable
fun SyncStatusText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.syncStatus,
    color: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(top = 2.dp)
    )
}

/**
 * Styled text for sensor name
 */
@Composable
fun SensorNameText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTypography.sensorName,
    color: Color = MaterialTheme.colors.onSurface,
    maxLines: Int = 1
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        modifier = modifier
    )
}

/**
 * Standard padding values - similar to CSS spacing utilities
 */
object AppSpacing {
    val xs = 2.dp
    val sm = 4.dp
    val md = 8.dp
    val lg = 10.dp
    val xl = 16.dp
    
    // Component-specific padding
    val deviceInfoTop = 2.dp
    val deviceInfoBottom = 4.dp
    val sensorChipHorizontal = 12.dp
    val sensorChipBottom = 6.dp
    val iconButtonPadding = 4.dp
}

/**
 * Standard component sizes
 */
object AppSizes {
    val iconButtonSmall = 32.dp
    val iconButtonMedium = 48.dp
    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 40.dp
    val sensorChipHeight = 32.dp
}

