package kaist.iclab.mobiletracker.repository


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
     * Returns the encapsulated data if this instance is [Success] or the
     * result of [onFailure] function for [Error]
     */
    inline fun getOrElse(onFailure: (exception: Throwable) -> @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            is Error -> onFailure(exception)
        }
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

