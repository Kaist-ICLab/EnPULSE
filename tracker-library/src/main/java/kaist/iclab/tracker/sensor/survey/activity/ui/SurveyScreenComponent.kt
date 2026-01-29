package kaist.iclab.tracker.sensor.survey.activity.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QuestionText(
    question: String,
    isMandatory: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        text = question + (if (isMandatory) "*" else ""),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun InputButtonRow(
    isRadioButton: Boolean,
    index: Int,
    optionDisplayText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowFreeResponse: Boolean = false,
    freeResponse: String = "",
    onFreeResponseChange: (String) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ).apply {
            if (allowFreeResponse) this.fillMaxWidth()
        }
    ) {
        if (isRadioButton) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(
            modifier = Modifier.width(8.dp)
        )
        Text(
            text = optionDisplayText ?: index.toString()
        )

        if (allowFreeResponse) {
            Spacer(
                modifier = Modifier.width(8.dp)
            )
            TextQuestionInput(
                value = freeResponse,
                onValueChange = onFreeResponseChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextQuestionInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowNumberOnly: Boolean = false
) {
    val initialValue = remember(value) { value }
    val interactionSource = remember { MutableInteractionSource() }
    val state = rememberTextFieldState(initialValue)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collectLatest {
            onValueChange(it)
        }
    }

    BasicTextField(
        state = state,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clearFocusOnKeyboardDismiss(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        inputTransformation = Transform@{
            if (!allowNumberOnly) return@Transform

            val input = asCharSequence().toString()
            if (input.toDoubleOrNull() != null) return@Transform
            if (input in listOf("", ".", "-")) return@Transform

            revertAllChanges()
        },
        decorator = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = 2.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        keyboardOptions = if (allowNumberOnly) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
    )
}

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeIsVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
            }
        }
    }
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}