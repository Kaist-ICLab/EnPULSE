package kaist.iclab.tracker.sensor.survey.activity.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.question.BinaryQuestion
import kaist.iclab.tracker.sensor.survey.question.MultipleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberScaleQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.SingleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kotlinx.serialization.json.JsonElement

@Composable
fun SurveyScreen(
    survey: Survey,
    pushSurveyResult: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val questionList = survey.flatQuestions
    val isAnswerValid = survey.isAnswerValid.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize()
    ) {
        items(questionList) { question ->
            when (question) {
                is BinaryQuestion -> BinaryQuestion(question)
                is SingleSelectionQuestion -> RadioQuestion(question)
                is MultipleSelectionQuestion -> CheckboxQuestion(question)
                is NumberScaleQuestion -> NumberScaleQuestion(question)
                is TextQuestion -> TextQuestion(question)
                is NumberQuestion -> NumberQuestion(question)
            }
        }
        item {
            Text(
                "*: mandatory question"
            )
        }
        item {
            Button(
                onClick = { pushSurveyResult(survey.getSurveyResponse()) },
                enabled = isAnswerValid.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun BinaryQuestion(
    question: BinaryQuestion,
    modifier: Modifier = Modifier
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()

    if (isHidden.value) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(true, false).forEachIndexed { index, value ->
                val selected = (value == response.value)
                val onClick = { question.setResponse(value) }
                val displayText = if(value) "Yes" else "No"
                if (selected) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(displayText)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(displayText)
                    }
                }
            }
        }
    }
}

@Composable
fun RadioQuestion(
    question: SingleSelectionQuestion,
    modifier: Modifier = Modifier
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()
    val otherResponse = question.otherResponse.collectAsState()

    if (isHidden.value) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()

    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        question.option.forEachIndexed { index, option ->
            InputButtonRow(
                isRadioButton = true,
                index = index,
                optionDisplayText = option,
                selected = (index == response.value),
                onClick = { question.setResponse(index) },
            )
        }
        question.freeResponseIndex?.let { index ->
            InputButtonRow(
                isRadioButton = true,
                index = index,
                optionDisplayText = question.freeResponsePrefix,
                selected = (index == response.value),
                onClick = { question.setResponse(index) },
                allowFreeResponse = true,
                freeResponse = otherResponse.value,
                onFreeResponseChange = { question.setOtherResponse(it) }
            )
        }
    }
}

@Composable
fun CheckboxQuestion(
    question: MultipleSelectionQuestion,
    modifier: Modifier = Modifier
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()
    val otherResponse = question.otherResponse.collectAsState()

    if (isHidden.value) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        question.option.forEachIndexed { index, option ->
            val selected = (index in response.value)
            InputButtonRow(
                isRadioButton = false,
                index = index,
                optionDisplayText = option,
                selected = selected,
                onClick = { question.toggleResponse(index, !selected) },
            )
        }
        question.freeResponseIndex?.let { index ->
            val selected = (index in response.value)
            InputButtonRow(
                isRadioButton = false,
                index = index,
                optionDisplayText = question.freeResponsePrefix,
                selected = selected,
                onClick = { question.toggleResponse(index, !selected) },
                allowFreeResponse = true,
                freeResponse = otherResponse.value,
                onFreeResponseChange = { question.setOtherResponse(it) }
            )
        }
    }
}

@Composable
fun TextQuestion(
    question: TextQuestion,
    modifier: Modifier = Modifier,
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()

    if (isHidden.value) return

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        TextQuestionInput(
            value = response.value,
            onValueChange = { question.setResponse(it) },
            allowNumberOnly = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberScaleQuestion(
    question: NumberScaleQuestion,
    modifier: Modifier = Modifier,
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()

    if (isHidden.value) return

    val selected = response.value ?: 0.0
    val thumbRadius = 10.dp
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        Slider(
            value = selected.toFloat(),
            onValueChange = { question.setResponse(it.toInt()) },
            valueRange = question.min.toFloat()..question.max.toFloat(),
            steps = (question.max - question.min).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(width = 4.dp, height = 24.dp)
                )
            }
        )
        NumberScaleLabels(
            labels = (question.min..question.max).map { it.toString() },
            edgePadding = thumbRadius,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            NumberScaleMeaningLabel(
                text = question.minLabel,
                direction = ScaleLabelDirection.Left,
                modifier = Modifier.weight(1f)
            )
            NumberScaleMeaningLabel(
                text = question.maxLabel,
                direction = ScaleLabelDirection.Right,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NumberScaleLabels(
    labels: List<String>,
    edgePadding: Dp,
    modifier: Modifier = Modifier
) {
    Layout(
        content = {
            labels.forEach {
                Text(
                    text = it,
                    textAlign = TextAlign.Center
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val padding = edgePadding.roundToPx()
        val travelWidth = (width - padding * 2).coerceAtLeast(0)
        val maxIndex = (placeables.size - 1).coerceAtLeast(1)

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val centerX = padding + travelWidth * index / maxIndex
                val x = centerX - placeable.width / 2
                placeable.placeRelative(x, 0)
            }
        }
    }
}

private enum class ScaleLabelDirection { Left, Right }

@Composable
private fun NumberScaleMeaningLabel(
    text: String,
    direction: ScaleLabelDirection,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.onBackground
    val content = @Composable {
        ScaleDirectionIcon(direction = direction, color = color)
        Text(text = text)
    }

    Row(
        horizontalArrangement = when (direction) {
            ScaleLabelDirection.Left -> Arrangement.Start
            ScaleLabelDirection.Right -> Arrangement.End
        },
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (direction == ScaleLabelDirection.Left) content()
        else {
            Text(text = text, textAlign = TextAlign.End)
            ScaleDirectionIcon(direction = direction, color = color)
        }
    }
}

@Composable
private fun ScaleDirectionIcon(
    direction: ScaleLabelDirection,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val centerY = size.height / 2f
        val startX = size.width * 0.2f
        val endX = size.width * 0.8f
        val tipX = if (direction == ScaleLabelDirection.Left) startX else endX
        val tailX = if (direction == ScaleLabelDirection.Left) endX else startX

        drawLine(
            color = color,
            start = Offset(tailX, centerY),
            end = Offset(tipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(tipX, centerY),
            end = Offset(
                if (direction == ScaleLabelDirection.Left) tipX + size.width * 0.22f else tipX - size.width * 0.22f,
                centerY - size.height * 0.22f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(tipX, centerY),
            end = Offset(
                if (direction == ScaleLabelDirection.Left) tipX + size.width * 0.22f else tipX - size.width * 0.22f,
                centerY + size.height * 0.22f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun NumberQuestion(
    question: NumberQuestion,
    modifier: Modifier = Modifier,
) {
    val response = question.response.collectAsState()
    val isHidden = question.isHidden.collectAsState()

    if (isHidden.value) return

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        QuestionText(
            question = question.question,
            isMandatory = question.isMandatory
        )
        TextQuestionInput(
            value = response.value.run { this?.toString() ?: "" },
            onValueChange = { question.setResponse(it.toDoubleOrNull()) },
            allowNumberOnly = true
        )
    }
}
