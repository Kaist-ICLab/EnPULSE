package kaist.iclab.wearabletracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppSpacing

@Composable
fun SettingController(
    upload: () -> Unit,
    flush: () -> Unit,
    startLogging: () -> Unit,
    stopLogging: () -> Unit,
    isCollecting: Boolean,
    hasEnabledSensors: Boolean
) {
    // Start button requires at least one sensor enabled
    // Connection check is done in startLogging callback
    val canStartCollection = hasEnabledSensors

    Row(
        modifier = Modifier
            .fillMaxWidth(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.Default.Upload,
            onClick = upload,
            contentDescription = stringResource(R.string.upload_data),
            backgroundColor = MaterialTheme.colors.secondary,
            buttonSize = AppSizes.iconButtonSmall,
            iconSize = AppSizes.iconSmall
        )
        IconButton(
            icon = if (isCollecting) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            onClick = {
                if (isCollecting) {
                    stopLogging()
                } else if (canStartCollection) {
                    startLogging()
                }
                // Do nothing when disabled
            },
            contentDescription = stringResource(R.string.start_stop_collection),
            backgroundColor = when {
                isCollecting -> MaterialTheme.colors.error
                canStartCollection -> MaterialTheme.colors.primary
                else -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f) // Greyed out
            },
            buttonSize = AppSizes.iconButtonMedium,
            iconSize = AppSizes.iconLarge,
        )
        IconButton(
            icon = Icons.Default.Delete,
            onClick = flush,
            contentDescription = stringResource(R.string.reset_icon),
            backgroundColor = MaterialTheme.colors.secondary,
            buttonSize = AppSizes.iconButtonSmall,
            iconSize = AppSizes.iconSmall
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
