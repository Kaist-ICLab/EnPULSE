package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

/**
 * Global Action Buttons (Submit/Dismiss)
 */
@Composable
internal fun ActionButtonGroup(
    showConfirm: Boolean = true,
    isConfirmEnabled: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = DarkSurface),
            shape = CircleShape
        ) {
            Text(
                text = "✕",
                fontSize = 14.sp,
                color = MutedGrey,
                fontWeight = FontWeight.Medium
            )
        }

        if (showConfirm) {
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isConfirmEnabled) AccentBlue else DarkSurface.copy(alpha = 0.4f),
                    disabledBackgroundColor = DarkSurface.copy(alpha = 0.4f),
                    contentColor = if (isConfirmEnabled) Color.Black else MutedGrey,
                    disabledContentColor = MutedGrey
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
