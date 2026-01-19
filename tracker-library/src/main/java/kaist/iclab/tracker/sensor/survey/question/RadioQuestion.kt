package kaist.iclab.tracker.sensor.survey.question

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RadioQuestion(
    override val question: String,
    override val isMandatory: Boolean,
    val option: List<Option>,
    questionTrigger: List<QuestionTrigger<String>>? = null
): Question<String>(
    question, isMandatory, "", questionTrigger
) {
    private val _otherResponse = MutableStateFlow<Map<String, String>>(mapOf())
    val otherResponse = _otherResponse.asStateFlow()

    init {
        _otherResponse.value = option.filter { it.allowFreeResponse }.associate { it.value to "" }
    }

    override fun isAllowedResponse(response: String): Boolean {
        val optionValues = option.map { it.value }
        return (response === "") || (response in optionValues)
    }

    override fun isEmpty(response: String) = (response == "")


    fun setOtherResponse(optionValue: String, response: String) {
        _otherResponse.value = otherResponse.value.toMutableMap().apply {
            this[optionValue] = response
        }
    }

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("question", question)
            put("isMandatory", isMandatory)
            put("value", response.value)
            if(response.value in otherResponse.value.keys) put("otherResponse", otherResponse.value[response.value])
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse("")
        _otherResponse.value = option.filter { it.allowFreeResponse }.associate { it.value to "" }
    }
}