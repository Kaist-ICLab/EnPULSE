package kaist.iclab.tracker.sensor.survey.question

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.collections.mapOf

class CheckboxQuestion(
    override val id: Int,
    override val question: String,
    override val isMandatory: Boolean,
    val option: List<Option>,
    questionTrigger: List<QuestionTrigger<Set<Int>>>? = null
): Question<Set<Int>>(
    id, question, isMandatory, setOf(), questionTrigger
) {
    private val _otherResponse = MutableStateFlow<Map<Int, String>>(mapOf())
    val otherResponse = _otherResponse.asStateFlow()

    init {
        _otherResponse.value = option.indices.associateWith { "" }.filter { option[it.key].allowFreeResponse }
    }

    override fun isAllowedResponse(response: Set<Int>): Boolean {
        return response.all { it in option.indices }
    }

    override fun isEmpty(response: Set<Int>) = response.isEmpty()

    fun toggleResponse(responseItemIdx: Int, isChecked: Boolean) {
        val newResponse = this.response.value.toMutableSet()
        newResponse.apply {
            if(isChecked) add(responseItemIdx)
            else remove(responseItemIdx)
        }

        setResponse(newResponse)
    }

    fun setOtherResponse(optionIdx: Int, response: String) {
        _otherResponse.value = otherResponse.value.toMutableMap().apply {
            this[optionIdx] = response
        }
    }

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("id", id)
            put("isMandatory", isMandatory)
            putJsonArray("response") {
                response.value.forEach {
                    add(buildJsonObject {
                        put("value", it)
                        if(it in otherResponse.value.keys) put("otherResponse", otherResponse.value[it])
                    })
                }
            }
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse(setOf())
        _otherResponse.value = option.indices.associateWith { "" }.filter { option[it.key].allowFreeResponse }
    }

    override fun eval(expr: Expression<Set<Int>>, value: Set<Int>): Boolean =
        when (expr) {
            is Predicate.Equal -> expr.value == value
            is Predicate.NotEqual -> expr.value != value

            is SetPredicate.Contains<*, *> -> value.contains(expr.value)

            is Operator.And -> eval(expr.a, value) && eval(expr.b, value)
            is Operator.Or  -> eval(expr.a, value) || eval(expr.b, value)
            is Operator.Not -> !eval(expr.a, value)

            else -> error("Unreachable")
        }
}