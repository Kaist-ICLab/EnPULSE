package kaist.iclab.tracker.sensor.survey.question

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.collections.toMutableMap

class RadioQuestion(
    override val id: Int,
    override val question: String,
    override val isMandatory: Boolean,
    val option: List<Option>,
    questionTrigger: List<QuestionTrigger<Int?>>? = null
): Question<Int?>(
    id, question, isMandatory, null, questionTrigger
) {
    private val _otherResponse = MutableStateFlow<Map<Int, String>>(mapOf())
    val otherResponse = _otherResponse.asStateFlow()

    init {
        _otherResponse.value = option.indices.associateWith { "" }.filter { option[it.key].allowFreeResponse }
    }

    override fun isAllowedResponse(response: Int?): Boolean {
        val optionValues = option.indices
        return (response === null) || (response in optionValues)
    }

    override fun isEmpty(response: Int?) = (response === null)


    fun setOtherResponse(optionIdx: Int, response: String) {
        _otherResponse.value = otherResponse.value.toMutableMap().apply {
            this[optionIdx] = response
        }
    }

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("id", id)
            put("isMandatory", isMandatory)
            put("value", response.value)
            if(response.value in otherResponse.value.keys) put("otherResponse", otherResponse.value[response.value])
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse(null)
        _otherResponse.value = option.indices.associateWith { "" }.filter { option[it.key].allowFreeResponse }
    }

    override fun eval(expr: Expression<Int?>, value: Int?): Boolean {
        if (value == null) return false
        return when (expr) {
            is Predicate.Equal -> expr.value == value
            is Predicate.NotEqual -> expr.value != value

            is ComparablePredicate.GreaterThan -> value > expr.value!!
            is ComparablePredicate.GreaterThanOrEqual -> value >= expr.value!!
            is ComparablePredicate.LessThan -> value < expr.value!!
            is ComparablePredicate.LessThanOrEqual -> value <= expr.value!!

            is Operator.And -> eval(expr.a, value) && eval(expr.b, value)
            is Operator.Or -> eval(expr.a, value) || eval(expr.b, value)
            is Operator.Not -> !eval(expr.a, value)

            else -> error("Unreachable")
        }
    }
}