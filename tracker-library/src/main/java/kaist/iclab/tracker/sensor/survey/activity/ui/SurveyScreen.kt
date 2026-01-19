package kaist.iclab.tracker.sensor.survey.activity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.question.CheckboxQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.RadioQuestion
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
                is RadioQuestion -> RadioQuestion(question)
                is CheckboxQuestion -> CheckboxQuestion(question)
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
fun RadioQuestion(
    question: RadioQuestion,
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
                option = option.value,
                optionDisplayText = option.displayText,
                selected = (option.value == response.value),
                onClick = { question.setResponse(option.value) },
                allowFreeResponse = option.allowFreeResponse,
                freeResponse = otherResponse.value[option.value] ?: "",
                onFreeResponseChange = { question.setOtherResponse(option.value, it) }
            )
        }
    }
}

@Composable
fun CheckboxQuestion(
    question: CheckboxQuestion,
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
            val selected = (option.value in response.value)
            InputButtonRow(
                isRadioButton = false,
                option = option.value,
                optionDisplayText = option.displayText,
                selected = selected,
                onClick = { question.toggleResponse(option.value, !selected) },
                allowFreeResponse = option.allowFreeResponse,
                freeResponse = otherResponse.value[option.value] ?: "",
                onFreeResponseChange = { question.setOtherResponse(option.value, it) }
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
            value = response.value.toString(),
            onValueChange = { question.setResponse(it) },
            allowNumberOnly = false
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