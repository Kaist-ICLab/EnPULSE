package kaist.iclab.tracker.sensor.survey.question

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class MultipleSelectionQuestion(
    override val id: Int,
    override val question: String,
    override val isMandatory: Boolean,
    val option: List<String>,
    val allowFreeResponse: Boolean,
    val freeResponsePrefix: String = "",
    questionTrigger: List<QuestionTrigger<Set<Int>>>? = null
) : Question<Set<Int>>(
    id, question, isMandatory, setOf(), questionTrigger
) {
    val freeResponseIndex: Int? = if (allowFreeResponse) option.size else null

    private val _otherResponse = MutableStateFlow("")
    val otherResponse = _otherResponse.asStateFlow()

    override fun isAllowedResponse(response: Set<Int>): Boolean {
        val optionValues = option.indices + listOfNotNull(freeResponseIndex)
        return response.all { it in optionValues }
    }

    override fun isEmpty(response: Set<Int>) = response.isEmpty()

    fun toggleResponse(responseItemIdx: Int, isChecked: Boolean) {
        val newResponse = this.response.value.toMutableSet()
        newResponse.apply {
            if (isChecked) add(responseItemIdx)
            else remove(responseItemIdx)
        }

        setResponse(newResponse)
    }

    fun setOtherResponse(response: String) {
        _otherResponse.value = response
    }

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("id", id)
            put("isMandatory", isMandatory)
            putJsonArray("response") {
                response.value.forEach {
                    add(buildJsonObject {
                        put("value", it)
                        if (it == freeResponseIndex) put(
                            "otherResponse",
                            otherResponse.value
                        )
                    })
                }
            }
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse(setOf())
        _otherResponse.value = ""
    }

    override fun eval(expr: Expression<Set<Int>>, value: Set<Int>): Boolean =
        when (expr) {
            is Predicate.Equal -> expr.value == value
            is Predicate.NotEqual -> expr.value != value

            is SetPredicate.Contains<*, *> -> value.contains(expr.value)

            is Operator.And -> eval(expr.a, value) && eval(expr.b, value)
            is Operator.Or -> eval(expr.a, value) || eval(expr.b, value)
            is Operator.Not -> !eval(expr.a, value)

            else -> error("Unreachable")
        }
}
