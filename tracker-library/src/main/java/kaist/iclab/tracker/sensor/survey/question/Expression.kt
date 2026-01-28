package kaist.iclab.tracker.sensor.survey.question

sealed interface Expression<T>

sealed interface Predicate<T> : Expression<T> {
    data class Equal<T>(val value: T) : Predicate<T>
    data class NotEqual<T>(val value: T) : Predicate<T>
}

sealed interface ComparablePredicate<T> : Expression<T> where T : Comparable<T> {
    data class GreaterThan<T>(val value: T) : ComparablePredicate<T> where T : Comparable<T>
    data class GreaterThanOrEqual<T>(val value: T) : ComparablePredicate<T> where T : Comparable<T>
    data class LessThan<T>(val value: T) : ComparablePredicate<T> where T : Comparable<T>
    data class LessThanOrEqual<T>(val value: T) : ComparablePredicate<T> where T : Comparable<T>
}

sealed interface SetPredicate<E, S>: Expression<S> where S: Set<E> {
    data class Contains<E, S>(val value: E): SetPredicate<E, S> where S: Set<E>
}

sealed interface StringPredicate: Expression<String> {
    class Empty: StringPredicate
}

sealed interface Operator<T> : Expression<T> {
    data class And<T>(val a: Expression<T>, val b: Expression<T>) : Operator<T>
    data class Or<T>(val a: Expression<T>, val b: Expression<T>) : Operator<T>
    data class Not<T>(val a: Expression<T>) : Operator<T>
}