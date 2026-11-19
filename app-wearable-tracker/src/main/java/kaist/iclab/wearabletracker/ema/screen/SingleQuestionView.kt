package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Text
import kaist.iclab.tracker.sensor.microema.AnswerType
import kaist.iclab.tracker.sensor.microema.WatchQuestion

/**
 * Displays a single question with the appropriate input widget.
 */
@Composable
internal fun SingleQuestionView(
    question: WatchQuestion,
    onAnswer: (String) -> Unit,
    onDismiss: () -> Unit,
    onInputOpen: () -> Unit,
    onInputClosed: () -> Unit
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
            var fontSizeValue by remember { mutableFloatStateOf(13f) }
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

                AnswerType.NUMBERSCALE -> {
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
                        onResult = { enteredText = it },
                        onInputOpen = onInputOpen,
                        onInputClosed = onInputClosed
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
            val showConfirm = when (question.answerType) {
                AnswerType.BINARY -> true
                AnswerType.RADIO if question.options.size <= 2 -> true
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

                        AnswerType.NUMBERSCALE -> {
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
