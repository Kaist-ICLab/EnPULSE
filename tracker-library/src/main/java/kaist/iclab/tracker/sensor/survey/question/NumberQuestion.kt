package kaist.iclab.tracker.sensor.survey.question

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NumberQuestion(
    override val question: String,
    override val isMandatory: Boolean,
    questionTrigger: List<QuestionTrigger<Double?>>? = null
): Question<Double?>(
    question, isMandatory, null, questionTrigger
) {
    override fun isAllowedResponse(response: Double?): Boolean {
        return true
    }

    override fun isEmpty(response: Double?) = (response == null)

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("question", question)
            put("isMandatory", isMandatory)
            put("response", response.value)
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse(null)
    }
}