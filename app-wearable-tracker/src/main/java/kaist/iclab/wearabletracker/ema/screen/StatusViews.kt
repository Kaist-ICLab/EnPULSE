package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.wearabletracker.R

/**
 * Brief loading indicator while survey config loads.
 */
@Composable
internal fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.ema_loading),
            fontSize = 14.sp,
            color = MutedGrey
        )
    }
}

/**
 * Completion view shown briefly after answering, expiry, or dismissal.
 */
@Composable
internal fun CompletionView(status: ResponseStatus?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (icon, label, color) = when (status) {
            ResponseStatus.ANSWERED -> Triple("✓", stringResource(R.string.ema_done), ConfirmGreen)
            ResponseStatus.EXPIRED -> Triple("⏰", stringResource(R.string.ema_time_up), Color.White)
            ResponseStatus.DISMISSED -> Triple(
                "✕",
                stringResource(R.string.ema_dismissed),
                RejectGrey
            )

            null -> Triple("…", stringResource(R.string.ema_finishing), MutedGrey)
        }

        Text(text = icon, fontSize = 28.sp, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
