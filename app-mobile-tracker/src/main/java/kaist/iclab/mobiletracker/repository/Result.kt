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
     * Gets the exception if error, null otherwise
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> exception
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
    } catch (e: Throwable) {
        Result.Error(e)
    }
}

