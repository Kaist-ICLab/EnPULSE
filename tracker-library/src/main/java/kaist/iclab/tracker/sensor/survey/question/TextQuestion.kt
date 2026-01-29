package kaist.iclab.tracker.sensor.survey.question

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TextQuestion(
    override val id: Int,
    override val question: String,
    override val isMandatory: Boolean,
    questionTrigger: List<QuestionTrigger<String>>? = null
): Question<String>(
    id, question, isMandatory, "", questionTrigger
) {
    override fun isAllowedResponse(response: String): Boolean {
        return true
    }

    override fun isEmpty(response: String) = (response == "")

    override fun getResponseJson(): JsonElement {
        val jsonObject = buildJsonObject {
            put("id", id)
            put("isMandatory", isMandatory)
            put("response", response.value)
        }

        return jsonObject
    }

    override fun initResponse() {
        setResponse("")
    }

    override fun eval(expr: Expression<String>, value: String): Boolean = when (expr) {
        is Predicate.Equal -> expr.value == value
        is Predicate.NotEqual -> expr.value != value

        is StringPredicate.Empty -> value.isEmpty()

        is Operator.And -> eval(expr.a, value) && eval(expr.b, value)
        is Operator.Or -> eval(expr.a, value) || eval(expr.b, value)
        is Operator.Not -> !eval(expr.a, value)

        else -> error("Unreachable")
    }
}