package kaist.iclab.tracker.sensor.survey.question

data class QuestionTrigger<T>(
    val predicate: Expression<T>,
    val children: List<Question<*>>
)