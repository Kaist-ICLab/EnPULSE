package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kaist.iclab.tracker.sensor.microema.WatchOption
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Auto-detect whether options represent a Likert scale (all numeric) or categorical labels.
 */
internal fun isLikertScale(options: List<WatchOption>): Boolean =
    options.all { it.display.toDoubleOrNull() != null }

internal fun Modifier.rotarySelectionInput(
    enabled: Boolean,
    focusRequester: FocusRequester,
    onCounterClockwise: () -> Unit,
    onClockwise: () -> Unit
): Modifier {
    if (!enabled) return this

    fun handleScroll(scrollPixels: Float): Boolean {
        return when {
            scrollPixels > 0f -> {
                onClockwise()
                true
            }

            scrollPixels < 0f -> {
                onCounterClockwise()
                true
            }

            else -> false
        }
    }

    return onPreRotaryScrollEvent { event ->
        handleScroll(event.verticalScrollPixels)
    }
        .onRotaryScrollEvent { event ->
            handleScroll(event.verticalScrollPixels)
        }
        .focusRequester(focusRequester)
        .focusable()
}

@Composable
internal fun rememberActiveFocusRequester(
    enabled: Boolean,
    key: Any?
): FocusRequester {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled, key) {
        if (!enabled) return@LaunchedEffect

        repeat(3) { attempt ->
            delay((50L * (attempt + 1)).milliseconds)
            try {
                focusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                // The focus target can be attached one frame later during screen transitions.
            }
        }
    }

    return focusRequester
}
