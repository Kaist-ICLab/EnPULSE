package kaist.iclab.tracker.sensor.survey.question

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

sealed class Question<T>(
    open val question: String,
    open val isMandatory: Boolean,
    initialValue: T,
    private val questionTrigger: List<QuestionTrigger<T>>? = null,
) {
    val children = questionTrigger?.flatMap { q: QuestionTrigger<T> -> q.children } ?: listOf()

    private val _response = MutableStateFlow(initialValue)
    val response = _response.asStateFlow()

    private val _isHidden = MutableStateFlow(false)
    val isHidden = _isHidden.asStateFlow()

    private val childQuestionIsValid: List<StateFlow<Boolean>> = children.map { it.isValid }
    private val _isValid = MutableStateFlow(false)
    val isValid = _isValid.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            combine(_response, _isHidden, *childQuestionIsValid.toTypedArray()) {}.collect {
                setIsValid()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            response.collect { res ->
                Log.d("Question", "Response: $res")
                questionTrigger?.forEach { trigger ->
                    trigger.children.forEach { it.setIsHidden(!eval(trigger.predicate, res)) }
                }
            }
        }
    }

    fun setIsHidden(isHidden: Boolean) {
        _isHidden.value = isHidden
    }

    fun setResponse(response: T) {
        if(!isAllowedResponse(response)) throw IllegalArgumentException("Invalid response value: $response")
        _response.value = response
    }

    abstract fun isAllowedResponse(response: T): Boolean
    abstract fun isEmpty(response: T): Boolean
    abstract fun getResponseJson(): JsonElement
    abstract fun initResponse()

    private fun setIsValid() {
        if(isHidden.value || !isMandatory) _isValid.value = true
        else _isValid.value = !isEmpty(response.value)
    }

    private fun eval(expr: Expression<T>, value: T): Boolean {
        return when(expr) {
            is ValueComparator<T> -> when(expr) {
                is ValueComparator.Equal<T> -> expr.value == value
//                is ValueComparator.GreaterThan<T> -> expr.value > value
//                is ValueComparator.GreaterThanOrEqual<T> -> expr.value >= value
//                is ValueComparator.LessThan<T> -> expr.value < value
//                is ValueComparator.LessThanOrEqual<T> -> expr.value <= value
                is ValueComparator.NotEqual<T> -> expr.value != value
            }

            is Operator<T> -> when(expr) {
                is Operator.And<T> -> eval(expr.a, value) && eval(expr.b, value)
                is Operator.Not<T> -> !eval(expr.a, value)
                is Operator.Or<T> -> eval(expr.a, value) || eval(expr.b, value)
            }
        }
    }
}