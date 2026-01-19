package kaist.iclab.tracker.sensor.survey.question

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TextQuestion(
    override val question: String,
    override val isMandatory: Boolean,
    questionTrigger: List<QuestionTrigger<String>>? = null
) : Question<String>(
    question, isMandatory, "", questionTrigger
) {
    override fun isAllowedResponse(response: String): Boolean {
        return true
    }

    override fun isEmpty(response: String) = (response == "")

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("question", question)
            put("isMandatory", isMandatory)
            put("response", response.value)
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse("")
    }
}