package kaist.iclab.tracker.sensor.microema

/**
 * The question to ask after [lastId]: the next one in [questions], wrapping to the first at the
 * end. Also starts from the first when nothing has been asked yet or [lastId] was removed.
 */
fun nextQuestionInRotation(questions: List<WatchQuestion>, lastId: Int): WatchQuestion? {
    if (questions.isEmpty()) return null
    val lastIndex = questions.indexOfFirst { it.id == lastId }
    return questions.getOrNull(lastIndex + 1) ?: questions.first()
}
