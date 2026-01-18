package kaist.iclab.tracker.sensor.survey.question

sealed class Expression<T>
sealed class ValueComparator<T>(): Expression<T>() {
    data class Equal<T>(val value: T): ValueComparator<T>()
    data class NotEqual<T>(val value: T): ValueComparator<T>()
//    data class GreaterThan<T: Comparable<T>>(val value: T): ValueComparator<T>()
//    data class GreaterThanOrEqual<T: Comparable<T>>(val value: T): ValueComparator<T>()
//    data class LessThan<T: Comparable<T>>(val value: T): ValueComparator<T>()
//    data class LessThanOrEqual<T: Comparable<T>>(val value: T): ValueComparator<T>()
}

sealed class Operator<T>: Expression<T>() {
    data class And<T: Comparable<T>>(val a: ValueComparator<T>, val b: ValueComparator<T>): Operator<T>()
    data class Or<T: Comparable<T>>(val a: ValueComparator<T>, val b: ValueComparator<T>): Operator<T>()
    data class Not<T: Comparable<T>>(val a: ValueComparator<T>): Operator<T>()
}