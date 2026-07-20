package kaist.iclab.wearabletracker.ema


import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.tooling.preview.devices.WearDevices
import kaist.iclab.tracker.sensor.microema.AnswerType
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.tracker.sensor.microema.WatchOption
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

// --- Color Palette ---
private val AccentBlue = Color(0xFF8AB4F8)
private val ConfirmGreen = Color(0xFF34A853)
private val RejectGrey = Color(0xFF5F6368)
private val MutedGrey = Color(0xFF9AA0A6)
private val DarkSurface = Color(0xFF3C4043)
private val TimerSafe = Color(0xFF81C995)
private val TimerWarning = Color(0xFFF4B400)
private val TimerCritical = Color(0xFFF28B82)

/**
 * Auto-detect whether options represent a Likert scale (all numeric) or categorical labels.
 */
private fun isLikertScale(options: List<WatchOption>): Boolean =
    options.all { it.display.toDoubleOrNull() != null }

private fun Modifier.edgeSwipeInput(
    enabled: Boolean,
    onCounterClockwise: () -> Unit,
    onClockwise: () -> Unit
): Modifier {
    if (!enabled) return this

    return pointerInput(onCounterClockwise, onClockwise) {
        val edgeWidthPx = 34.dp.toPx()
        val triggerAngle = (PI / 6.0).toFloat()

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val outerRadius = minOf(size.width, size.height) / 2f
            val downRadius = hypot(down.position.x - centerX, down.position.y - centerY)
            if (downRadius < outerRadius - edgeWidthPx) return@awaitEachGesture

            var previousAngle = atan2(down.position.y - centerY, down.position.x - centerX)
            var accumulatedAngle = 0f
            var triggered = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val position = change.position
                val currentRadius = hypot(position.x - centerX, position.y - centerY)
                if (currentRadius < outerRadius - edgeWidthPx * 1.6f) break

                val currentAngle = atan2(position.y - centerY, position.x - centerX)
                var delta = currentAngle - previousAngle
                if (delta > PI) delta -= (2 * PI).toFloat()
                if (delta < -PI) delta += (2 * PI).toFloat()
                previousAngle = currentAngle
                accumulatedAngle += delta

                when {
                    accumulatedAngle >= triggerAngle -> {
                        onClockwise()
                        accumulatedAngle = 0f
                        triggered = true
                        change.consume()
                    }

                    accumulatedAngle <= -triggerAngle -> {
                        onCounterClockwise()
                        accumulatedAngle = 0f
                        triggered = true
                        change.consume()
                    }

                    triggered || abs(delta) > 0.01f -> {
                        change.consume()
                    }
                }
            }
        }
    }
}

private fun Modifier.rotarySelectionInput(
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
private fun rememberActiveFocusRequester(
    enabled: Boolean,
    key: Any?
): FocusRequester {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled, key) {
        if (!enabled) return@LaunchedEffect

        repeat(3) { attempt ->
            delay(50L * (attempt + 1))
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
            delay(3000L) // show the "Done" state for 3s
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
        onDismiss = { viewModel.dismiss() }
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
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Bezel Timer (Circular Progress)
        if (!isComplete && remainingTimeMs != null) {
            val totalTime = config?.expireAfterMs ?: 30000L
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
                        onDismiss = onDismiss
                    )
                } ?: LoadingView()
            }
        }
    }
}

/**
 * Displays a single question with the appropriate input widget.
 */
@Composable
private fun SingleQuestionView(
    question: WatchQuestion,
    onAnswer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val numberOptions = remember(question) {
        question.options.map { it.display }.ifEmpty { (0..10).map { it.toString() } }
    }
    val initialSelectedIndex = remember(question) {
        if (question.options.isEmpty()) {
            0
        } else if (question.answerType == AnswerType.BINARY ||
            (question.answerType == AnswerType.RADIO && question.options.size <= 2)
        ) {
            0
        } else {
            question.options.size / 2
        }
    }
    var selectedIndex by remember(question) { mutableIntStateOf(initialSelectedIndex) }
    var selectedIds by remember(question) { mutableStateOf(emptySet<Int>()) }
    var selectedNumberIndex by remember(question) { mutableIntStateOf(numberOptions.lastIndex / 2) }
    var enteredText by remember(question) { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val isLikert = remember(question) { isLikertScale(question.options) }
    val edgeSwipeEnabled = when (question.answerType) {
        AnswerType.BINARY,
        AnswerType.RADIO,
        AnswerType.NUMBERSCALE -> true

        else -> false
    }
    val focusRequester = rememberActiveFocusRequester(
        enabled = edgeSwipeEnabled,
        key = question.id
    )

    fun selectPrevious() {
        var didChange = false
        when (question.answerType) {
            AnswerType.BINARY -> {
                val nextIndex = (selectedIndex - 1).coerceAtLeast(0)
                didChange = nextIndex != selectedIndex
                selectedIndex = nextIndex
            }

            AnswerType.RADIO -> {
                val nextIndex = (selectedIndex - 1).coerceAtLeast(0)
                didChange = nextIndex != selectedIndex
                selectedIndex = nextIndex
            }

            AnswerType.NUMBERSCALE -> {
                val nextIndex = (selectedNumberIndex - 1).coerceAtLeast(0)
                didChange = nextIndex != selectedNumberIndex
                selectedNumberIndex = nextIndex
            }

            else -> Unit
        }
        if (didChange) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun selectNext() {
        var didChange = false
        when (question.answerType) {
            AnswerType.BINARY -> {
                if (question.options.isNotEmpty()) {
                    val nextIndex = (selectedIndex + 1).coerceAtMost(question.options.lastIndex)
                    didChange = nextIndex != selectedIndex
                    selectedIndex = nextIndex
                }
            }

            AnswerType.RADIO -> {
                if (question.options.isNotEmpty()) {
                    val nextIndex = (selectedIndex + 1).coerceAtMost(question.options.lastIndex)
                    didChange = nextIndex != selectedIndex
                    selectedIndex = nextIndex
                }
            }

            AnswerType.NUMBERSCALE -> {
                val nextIndex = (selectedNumberIndex + 1).coerceAtMost(numberOptions.lastIndex)
                didChange = nextIndex != selectedNumberIndex
                selectedNumberIndex = nextIndex
            }

            else -> Unit
        }
        if (didChange) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .rotarySelectionInput(
                enabled = edgeSwipeEnabled,
                focusRequester = focusRequester,
                onCounterClockwise = ::selectPrevious,
                onClockwise = ::selectNext
            )
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Reduced top margin to lift the question slightly higher
        Spacer(modifier = Modifier.height(30.dp))

        // --- SECTION 1: Top (Question) ---
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            var fontSizeValue by remember { mutableStateOf(13f) }
            var readyToDraw by remember { mutableStateOf(false) }

            Text(
                text = question.text,
                fontSize = fontSizeValue.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                lineHeight = (fontSizeValue * 1.3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .drawWithContent { if (readyToDraw) drawContent() },
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.hasVisualOverflow && fontSizeValue > 10f) {
                        fontSizeValue -= 0.5f
                    } else {
                        readyToDraw = true
                    }
                }
            )
        }

        // --- SECTION 2: Middle (Interaction / Options) ---
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            when (question.answerType) {
                AnswerType.RADIO -> {
                    if (question.options.size <= 2) {
                        TapButtonsInput(
                            options = question.options,
                            selectedIndex = selectedIndex,
                            onSelect = onAnswer
                        )
                    } else if (isLikert) {
                        HorizontalOptionInput(
                            options = question.options,
                            selectedIndex = selectedIndex,
                            onIndexChanged = { selectedIndex = it }
                        )
                    } else {
                        CategoricalOptionInput(
                            options = question.options,
                            selectedIndex = selectedIndex,
                            onIndexChanged = { selectedIndex = it }
                        )
                    }
                }

                AnswerType.CHECKBOX -> {
                    CheckboxInput(
                        options = question.options,
                        selectedIds = selectedIds,
                        onToggle = { id ->
                            selectedIds = if (selectedIds.contains(id)) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                        }
                    )
                }

                AnswerType.BINARY -> {
                    TapButtonsInput(
                        options = question.options,
                        selectedIndex = selectedIndex,
                        onSelect = onAnswer
                    )
                }

                AnswerType.NUMBERSCALE, AnswerType.NUMBER -> {
                    NumberPickerInput(
                        numberOptions = numberOptions,
                        selectedIndex = selectedNumberIndex,
                        onIndexChanged = { selectedNumberIndex = it },
                        onSelect = onAnswer
                    )
                }

                AnswerType.TEXT -> {
                    TextInput(
                        currentText = enteredText,
                        onResult = { enteredText = it }
                    )
                }
            }
        }

        // --- SECTION 3: Bottom (Actions) ---
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Show confirm for Likert, Categorical, Checkbox, and Number/Text
            val showConfirm = when {
                question.answerType == AnswerType.BINARY -> true
                question.answerType == AnswerType.RADIO && question.options.size <= 2 -> true
                else -> true
            }

            val isConfirmEnabled = when (question.answerType) {
                AnswerType.TEXT -> enteredText.isNotBlank()
                AnswerType.CHECKBOX -> selectedIds.isNotEmpty()
                else -> true
            }

            ActionButtonGroup(
                showConfirm = showConfirm,
                isConfirmEnabled = isConfirmEnabled,
                onConfirm = {
                    when (question.answerType) {
                        AnswerType.RADIO -> {
                            if (question.options.isNotEmpty()) {
                                onAnswer(question.options[selectedIndex].display)
                            }
                        }

                        AnswerType.CHECKBOX -> {
                            if (selectedIds.isNotEmpty()) {
                                val selectedTexts = question.options
                                    .filter { it.id in selectedIds }
                                    .joinToString(", ") { it.display }
                                onAnswer(selectedTexts)
                            }
                        }

                        AnswerType.TEXT -> {
                            if (enteredText.isNotBlank()) {
                                onAnswer(enteredText)
                            }
                        }

                        AnswerType.NUMBERSCALE, AnswerType.NUMBER -> {
                            onAnswer(numberOptions[selectedNumberIndex])
                        }

                        AnswerType.BINARY -> {
                            if (question.options.isNotEmpty()) {
                                onAnswer(question.options[selectedIndex].display)
                            }
                        }
                    }
                },
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Horizontally scrollable option selector for RADIO questions with 3+ options.
 * Each option is a tappable chip; the selected one highlights in blue.
 * User scrolls left/right, then taps confirm.
 */
@Composable
private fun HorizontalOptionInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    val listState =
        rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex - 1))

    LaunchedEffect(selectedIndex) {
        if (options.isNotEmpty()) {
            val targetIndex = maxOf(0, selectedIndex - 1).coerceAtMost(options.lastIndex)
            listState.scrollToItem(targetIndex)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontally scrollable options
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            itemsIndexed(options) { index, option ->
                val isSelected = index == selectedIndex
                val bgColor = if (isSelected) AccentBlue else DarkSurface.copy(alpha = 0.6f)
                val textColor = if (isSelected) Color.Black else Color.White
                val borderMod = if (isSelected) {
                    Modifier.border(2.dp, AccentBlue.copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                }

                Button(
                    onClick = { onIndexChanged(index) },
                    modifier = Modifier
                        .size(width = 36.dp, height = 30.dp)
                        .then(borderMod),
                    colors = ButtonDefaults.buttonColors(backgroundColor = bgColor),
                    shape = CircleShape
                ) {
                    Text(
                        text = option.display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scale labels
        if (options.size >= 3) {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.ema_low), fontSize = 10.sp, color = MutedGrey)
                Text(text = stringResource(R.string.ema_high), fontSize = 10.sp, color = MutedGrey)
            }
        }
    }
}

/**
 * Horizontal pager showing one categorical option at a time.
 * User swipes left/right to browse options; the visible option is always selected.
 */
@Composable
private fun CategoricalOptionInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { options.size }
    )

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    // Sync pager scroll → selectedIndex
    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pager showing one option at a time
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            pageSpacing = 8.dp
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = options[page].display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(options.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 6.dp else 4.dp)
                        .background(
                            color = if (index == pagerState.currentPage) AccentBlue else MutedGrey.copy(
                                alpha = 0.5f
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Vertically scrollable list of categorical options with checkboxes (multi-select).
 */
@Composable
private fun CheckboxInput(
    options: List<WatchOption>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        itemsIndexed(options) { index: Int, option: WatchOption ->
            val isChecked = selectedIds.contains(option.id)
            ToggleChip(
                checked = isChecked,
                onCheckedChange = { onToggle(option.id) },
                label = {
                    Text(
                        text = option.display,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                toggleControl = {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle(option.id) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = AccentBlue.copy(alpha = 0.2f),
                    checkedEndBackgroundColor = AccentBlue.copy(alpha = 0.2f),
                    uncheckedStartBackgroundColor = DarkSurface.copy(alpha = 0.6f),
                    uncheckedEndBackgroundColor = DarkSurface.copy(alpha = 0.6f),
                    checkedContentColor = Color.White,
                    uncheckedContentColor = Color.White,
                    checkedToggleControlColor = AccentBlue
                )
            )
        }
    }
}

/**
 * Button that launches system remote input (keyboard/voice/handwriting).
 */
@Composable
private fun TextInput(
    currentText: String,
    onResult: (String) -> Unit
) {
    // 1. Launcher for Unified Input (Keyboard/Handwriting/Voice)
    val textLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val bundle = RemoteInput.getResultsFromIntent(data)
            val text = bundle?.getCharSequence("ema_text_input")?.toString()
            if (!text.isNullOrBlank()) {
                onResult(text)
            }
        }
    }

    // 2. Launcher for Direct Voice-only Input (STT)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val firstResult = results?.firstOrNull()
            if (!firstResult.isNullOrBlank()) {
                onResult(firstResult)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Manual Text Button (Keyboard) ---
        Button(
            onClick = {
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs = listOf(
                    RemoteInput.Builder("ema_text_input")
                        .setLabel("Enter your answer")
                        .build()
                )
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                textLauncher.launch(intent)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(backgroundColor = DarkSurface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Type",
                    modifier = Modifier.size(14.dp),
                    tint = MutedGrey
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = currentText.ifBlank { "Tap to type..." },
                    fontSize = 11.sp,
                    color = if (currentText.isBlank()) MutedGrey else Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- Direct Mic Button ---
        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                }
                voiceLauncher.launch(intent)
            },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Speak",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * Two large tap buttons for ≤2 options (Yes/No, binary choices).
 */
@Composable
private fun TapButtonsInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onSelect: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Button(
                    onClick = { onSelect(option.display) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSelected) AccentBlue else DarkSurface
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = option.display,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Scrollable number picker (0–10 range).
 */
@Composable
private fun NumberPickerInput(
    numberOptions: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onSelect: (String) -> Unit
) {
    val pickerState = rememberPickerState(
        initialNumberOfOptions = numberOptions.size,
        initiallySelectedOption = selectedIndex
    )

    LaunchedEffect(selectedIndex) {
        if (pickerState.selectedOption != selectedIndex) {
            pickerState.scrollToOption(selectedIndex)
        }
    }

    // Sync picker scroll → onValueChange
    LaunchedEffect(pickerState.selectedOption) {
        onIndexChanged(pickerState.selectedOption)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Picker(
            state = pickerState,
            modifier = Modifier.size(width = 80.dp, height = 60.dp),
            separation = 4.dp,
            contentDescription = ""
        ) { index ->
            Text(
                text = numberOptions[index],
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = numberOptions.first(), fontSize = 10.sp, color = MutedGrey)
            Text(text = numberOptions.last(), fontSize = 10.sp, color = MutedGrey)
        }
    }
}

/**
 * Global Action Buttons (Submit/Dismiss)
 */
@Composable
private fun ActionButtonGroup(
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

/**
 * Brief loading indicator while survey config loads.
 */
@Composable
private fun LoadingView() {
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
private fun CompletionView(status: ResponseStatus?) {
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

// --- Previews ---

private val previewBinaryQuestion = WatchQuestion(
    id = 1,
    surveyId = 1,
    text = "Are you feeling stressed right now?",
    answerType = AnswerType.BINARY,
    options = listOf(WatchOption(1, "No"), WatchOption(2, "Yes"))
)

private val previewLikertQuestion = WatchQuestion(
    id = 2,
    surveyId = 1,
    text = "How would you rate your current mood?",
    answerType = AnswerType.RADIO,
    options = (1..5).map { WatchOption(it, it.toString()) }
)

private val previewCategoricalQuestion = WatchQuestion(
    id = 3,
    surveyId = 1,
    text = "Where are you right now?",
    answerType = AnswerType.RADIO,
    options = listOf(
        WatchOption(1, "Home"),
        WatchOption(2, "Work"),
        WatchOption(3, "Commuting"),
        WatchOption(4, "Outdoors")
    )
)

private val previewCheckboxQuestion = WatchQuestion(
    id = 4,
    surveyId = 1,
    text = "Which activities have you done in the last hour?",
    answerType = AnswerType.CHECKBOX,
    options = listOf(
        WatchOption(1, "Walking"),
        WatchOption(2, "Eating"),
        WatchOption(3, "Working"),
        WatchOption(4, "Resting")
    )
)

private val previewNumberQuestion = WatchQuestion(
    id = 5,
    surveyId = 1,
    text = "Rate your energy level right now",
    answerType = AnswerType.NUMBERSCALE
)

private val previewTextQuestion = WatchQuestion(
    id = 6,
    surveyId = 1,
    text = "Anything else you'd like to note?",
    answerType = AnswerType.TEXT
)

private fun previewConfig(question: WatchQuestion) = WatchSurveyConfig(
    surveyId = 1,
    title = "Mood Check",
    expireAfterMs = 30_000L,
    questions = listOf(question)
)

@Preview(name = "Binary", device = WearDevices.SMALL_ROUND, showBackground = true)
@Preview(name = "Binary - Large Round", device = WearDevices.LARGE_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenBinaryPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewBinaryQuestion,
            config = previewConfig(previewBinaryQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = 22_000L,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Likert Scale", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenLikertPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewLikertQuestion,
            config = previewConfig(previewLikertQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = 15_000L,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Categorical", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenCategoricalPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewCategoricalQuestion,
            config = previewConfig(previewCategoricalQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = 8_000L,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Checkbox", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenCheckboxPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewCheckboxQuestion,
            config = previewConfig(previewCheckboxQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = 30_000L,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Number Picker", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenNumberPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewNumberQuestion,
            config = previewConfig(previewNumberQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = 30_000L,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Text Input", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenTextPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewTextQuestion,
            config = previewConfig(previewTextQuestion),
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = null,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Loading", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenLoadingPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = null,
            config = null,
            isComplete = false,
            finalStatus = null,
            remainingTimeMs = null,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Answered", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenAnsweredPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewBinaryQuestion,
            config = previewConfig(previewBinaryQuestion),
            isComplete = true,
            finalStatus = ResponseStatus.ANSWERED,
            remainingTimeMs = null,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Expired", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenExpiredPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewBinaryQuestion,
            config = previewConfig(previewBinaryQuestion),
            isComplete = true,
            finalStatus = ResponseStatus.EXPIRED,
            remainingTimeMs = null,
            onAnswer = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Dismissed", device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun MicroEmaScreenDismissedPreview() {
    WearableTrackerTheme {
        MicroEmaScreenContent(
            question = previewBinaryQuestion,
            config = previewConfig(previewBinaryQuestion),
            isComplete = true,
            finalStatus = ResponseStatus.DISMISSED,
            remainingTimeMs = null,
            onAnswer = {},
            onDismiss = {}
        )
    }
}
