package kaist.iclab.wearabletracker.ecg

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import kaist.iclab.wearabletracker.R
import kotlinx.coroutines.delay

private val TimerSafe = Color(0xFF81C995)
private val TimerWarning = Color(0xFFF4B400)
private val TimerCritical = Color(0xFFF28B82)
private val ConfirmGreen = Color(0xFF34A853)
private val MutedGrey = Color(0xFF9AA0A6)

/**
 * Screen for a single on-demand ECG measurement.
 *
 * States:
 * 1. Checking — verifying sensor availability/permission
 * 2. Running — 30s countdown with a bezel progress ring, reinforcing the elbow-placement instruction
 * 3. Completed / Unavailable / NeedsPermission — brief message, then auto-closes
 */
@Composable
fun EcgMeasurementScreen(
    viewModel: EcgViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startMeasurement()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is EcgMeasurementUiState.Completed,
            is EcgMeasurementUiState.Unavailable,
            is EcgMeasurementUiState.NeedsPermission -> {
                delay(3000L)
                onFinish()
            }

            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val running = uiState as? EcgMeasurementUiState.Running
        if (running != null) {
            val progress = (running.remainingMs.toFloat() / running.totalMs.toFloat()).coerceIn(0f, 1f)
            val color = when {
                progress > 0.6f -> TimerSafe
                progress > 0.3f -> TimerWarning
                else -> TimerCritical
            }

            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                startAngle = 270f,
                endAngle = 270f,
                indicatorColor = color,
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeWidth = 3.dp
            )
        }

        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ecg_state"
        ) { state ->
            when (state) {
                is EcgMeasurementUiState.Checking -> LoadingView()

                is EcgMeasurementUiState.Unavailable -> MessageView(
                    icon = "✕",
                    label = stringResource(R.string.ecg_unavailable),
                    color = MutedGrey
                )

                is EcgMeasurementUiState.NeedsPermission -> MessageView(
                    icon = "⚠",
                    label = stringResource(R.string.ecg_needs_permission),
                    color = TimerWarning
                )

                is EcgMeasurementUiState.Running -> RunningView(remainingMs = state.remainingMs)

                is EcgMeasurementUiState.Completed -> MessageView(
                    icon = "✓",
                    label = stringResource(R.string.ecg_completed),
                    color = ConfirmGreen
                )
            }
        }
    }
}

@Composable
private fun RunningView(remainingMs: Long) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.ecg_running_instruction),
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(remainingMs / 1000L) + 1}s",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun MessageView(icon: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 28.sp, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingView() {
    Text(
        text = stringResource(R.string.ema_loading),
        fontSize = 14.sp,
        color = MutedGrey
    )
}
