package kaist.iclab.tracker.sensor.survey.question

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BinaryQuestion(
    override val id: Int,
    override val question: String,
    override val isMandatory: Boolean,
    questionTrigger: List<QuestionTrigger<Boolean?>>? = null
) : Question<Boolean?>(
    id, question, isMandatory, null, questionTrigger
) {
    override fun isAllowedResponse(response: Boolean?): Boolean {
        return true
    }

    override fun isEmpty(response: Boolean?) = (response === null)

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("id", id)
            put("isMandatory", isMandatory)
            put("value", response.value)
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse(null)
    }

    override fun eval(expr: Expression<Boolean?>, value: Boolean?): Boolean {
        if (value == null) return false
        return when (expr) {
            is Predicate.Equal -> expr.value == value
            is Predicate.NotEqual -> expr.value != value

            is Operator.And -> eval(expr.a, value) && eval(expr.b, value)
            is Operator.Or -> eval(expr.a, value) || eval(expr.b, value)
            is Operator.Not -> !eval(expr.a, value)

            else -> error("Unreachable")
        }
    }
}
