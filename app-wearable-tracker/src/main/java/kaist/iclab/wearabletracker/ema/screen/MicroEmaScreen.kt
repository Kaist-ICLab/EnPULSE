package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.wearabletracker.ema.MicroEmaViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Main microEMA screen — displays a single question and handles the response.
 *
 * States:
 * 1. Loading — survey config is being loaded
 * 2. Question — displays the single question with appropriate input widget
 * 3. Complete — brief success animation, then auto-closes
 */
@Composable
fun MicroEmaScreen(
    viewModel: MicroEmaViewModel,
    onFinish: () -> Unit
) {
    val question by viewModel.question.collectAsState()
    val config by viewModel.surveyConfig.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val finalStatus by viewModel.finalStatus.collectAsState()
    val remainingTimeMs by viewModel.remainingTimeMs.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Start survey on first composition
    LaunchedEffect(Unit) {
        viewModel.startSurvey()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Auto-close after completion with a brief delay
    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(3000L.milliseconds) // show the "Done" state for 3s
            onFinish()
        }
    }

    MicroEmaScreenContent(
        question = question,
        config = config,
        isComplete = isComplete,
        finalStatus = finalStatus,
        remainingTimeMs = remainingTimeMs,
        onAnswer = { viewModel.answerQuestion(it) },
        onDismiss = { viewModel.dismiss() },
        onInputOpen = { viewModel.pauseCountdown() },
        onInputClosed = { viewModel.resumeCountdown() }
    )
}

/**
 * Stateless rendering of the microEMA screen — every value is a plain parameter, so it doesn't
 * need a ViewModel and can be exercised directly from @Preview.
 */
@Composable
fun MicroEmaScreenContent(
    question: WatchQuestion?,
    config: WatchSurveyConfig?,
    isComplete: Boolean,
    finalStatus: ResponseStatus?,
    remainingTimeMs: Long?,
    onAnswer: (String) -> Unit,
    onDismiss: () -> Unit,
    onInputOpen: () -> Unit = {},
    onInputClosed: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Bezel Timer (Circular Progress)
        if (!isComplete && remainingTimeMs != null) {
            // Same fallback as MicroEmaViewModel, so a 0 expiry doesn't divide by zero.
            val totalTime = config?.expireAfterMs?.takeIf { it > 0 } ?: 30_000L
            val progress = (remainingTimeMs.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
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
            targetState = isComplete,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "survey_state"
        ) { complete ->
            if (complete) {
                CompletionView(status = finalStatus)
            } else {
                question?.let { q ->
                    SingleQuestionView(
                        question = q,
                        onAnswer = onAnswer,
                        onDismiss = onDismiss,
                        onInputOpen = onInputOpen,
                        onInputClosed = onInputClosed
                    )
                } ?: LoadingView()
            }
        }
    }
}
