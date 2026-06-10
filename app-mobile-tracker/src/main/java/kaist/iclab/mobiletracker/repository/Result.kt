package kaist.iclab.mobiletracker.repository

import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sealed class representing the result of a repository operation.
 * Provides type-safe error handling for repository methods.
 *
 * @param T The type of data returned on success
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with an error
     */
    data class Error(val exception: Throwable) : Result<Nothing>() {
        val message: String get() = exception.message ?: "Unknown error"
    }

    /**
     * Returns true if the result is a success
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if the result is an error
     */
    val isError: Boolean get() = this is Error

    /**
     * Gets the data if successful, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Gets the exception if error, null otherwise
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> exception
    }

    /**
     * Returns the encapsulated data if this instance is [Success] or the
     * result of [onFailure] function for [Error]
     */
    inline fun getOrElse(onFailure: (exception: Throwable) -> @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            is Error -> onFailure(exception)
        }
    }

    /**
     * Returns the encapsulated data if this instance is [Success] or [defaultValue]
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            is Error -> defaultValue
        }
    }
}

/**
 * Helper function to create a Result from a try-catch block
 */
inline fun <T> runCatching(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
        Result.Error(e)
    }
}

/**
 * Helper function to create a Result from a suspend function
 */
suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: TimeoutCancellationException) {
        // A real request timeout (from withTimeout) — surface it as a failure.
        Result.Error(e)
    } catch (e: CancellationException) {
        // Propagate genuine coroutine cancellation instead of swallowing it as a failure.
        throw e
    } catch (e: Throwable) {
        Result.Error(e)
    }
}

/**
 * Maps a Result<T> to Result<R> by applying a transform function to the success value.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> try {
            Result.Success(transform(data))
        } catch (e: Throwable) {
            Result.Error(e)
        }

        is Result.Error -> this
    }
}

/**
 * Performs the given action on the encapsulated value if this instance represents [Result.Success].
 * Returns the original Result unchanged.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Performs the given action on the encapsulated exception if this instance represents [Result.Error].
 * Returns the original Result unchanged.
 */
inline fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

/**
 * Chains a Result-returning operation on success, propagating errors.
 * Replaces the common `when (result) { is Success -> ...; is Error -> throw ... }` pattern.
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> try {
            transform(data)
        } catch (e: Throwable) {
            Result.Error(e)
        }

        is Result.Error -> this
    }
}

/**
 * Transforms the exception inside a [Result.Error] while leaving [Result.Success] unchanged.
 * Useful for classifying raw exceptions into typed [AppError] subtypes.
 */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> {
    return when (this) {
        is Result.Success -> this
        is Result.Error -> Result.Error(transform(exception))
    }
}
