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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kaist.iclab.tracker.sensor.microema.AnswerType
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.tracker.sensor.microema.WatchOption
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.wearabletracker.R
import kotlinx.coroutines.delay

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Bezel Timer (Circular Progress)
        if (!isComplete && remainingTimeMs != null) {
            val totalTime = config?.expireAfterMs ?: 30000L
            val progress = (remainingTimeMs!!.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
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
                        onAnswer = { viewModel.answerQuestion(it) },
                        onDismiss = { viewModel.dismiss() }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        var selectedIndex by remember { mutableIntStateOf(question.options.size / 2) }
        var selectedIds by remember { mutableStateOf(emptySet<Int>()) }
        var selectedNumber by remember { mutableStateOf("5") }
        var enteredText by remember { mutableStateOf("") }

        val isLikert = remember(question) { isLikertScale(question.options) }

        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            when (question.answerType) {
                AnswerType.RADIO -> {
                    if (question.options.size <= 2) {
                        TapButtonsInput(
                            options = question.options,
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
                        onSelect = onAnswer
                    )
                }

                AnswerType.NUMBERSCALE, AnswerType.NUMBER -> {
                    NumberPickerInput(
                        options = question.options,
                        onValueChange = { selectedNumber = it },
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
                question.answerType == AnswerType.BINARY -> false
                question.answerType == AnswerType.RADIO && question.options.size <= 2 -> false
                else -> true
            }

            ActionButtonGroup(
                showConfirm = showConfirm,
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
                            onAnswer(selectedNumber)
                        }

                        AnswerType.BINARY -> {
                            // Handled directly by TapButtonsInput
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
                Button(
                    onClick = { onSelect(option.display) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = DarkSurface),
                    shape = CircleShape
                ) {
                    Text(
                        text = option.display,
                        fontSize = 14.sp,
                        color = Color.White,
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
    options: List<WatchOption>,
    onValueChange: (String) -> Unit,
    onSelect: (String) -> Unit
) {
    val numberOptions = options.map { it.display }.ifEmpty { (0..10).map { it.toString() } }
    val pickerState = rememberPickerState(
        initialNumberOfOptions = numberOptions.size,
        initiallySelectedOption = numberOptions.lastIndex / 2
    )

    // Sync picker scroll → onValueChange
    LaunchedEffect(pickerState.selectedOption) {
        onValueChange(numberOptions[pickerState.selectedOption])
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
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                shape = CircleShape
            ) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    color = Color.Black,
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
