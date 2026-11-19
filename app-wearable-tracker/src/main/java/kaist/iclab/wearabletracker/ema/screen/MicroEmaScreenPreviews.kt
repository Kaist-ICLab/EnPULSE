package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import kaist.iclab.tracker.sensor.microema.AnswerType
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.tracker.sensor.microema.WatchOption
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme

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
