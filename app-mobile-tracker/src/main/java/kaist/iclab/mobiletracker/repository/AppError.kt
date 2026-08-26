package kaist.iclab.mobiletracker.repository

/**
 * Sealed class hierarchy for categorized application errors.
 *
 * Provides typed error categories so that callers can distinguish between
 * network failures, auth issues, database problems, etc. without inspecting
 * raw exception types or messages.
 *
 * All subtypes extend [Exception] so they can be used anywhere a [Throwable]
 * is expected, including inside [Result.Error].
 */
sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Network/connectivity issues (timeout, no host, connection refused). */
    class Network(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Authentication or authorization failures. */
    class Auth(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Database read/write failures. */
    class Database(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Requested resource was not found. */
    class NotFound(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Invalid input or parameters. */
    class Validation(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Operation exceeded the allowed time limit. */
    class Timeout(message: String, cause: Throwable? = null) : AppError(message, cause)

    /**
     * The server received the request and explicitly rejected it — an RLS policy, a constraint
     * violation, a malformed payload, etc. Unlike [Network]/[Timeout]/[Auth], this is a decisive
     * answer: retrying the exact same data will keep failing the exact same way, which is what
     * upload code uses to tell "this data is the problem" apart from "try again later".
     */
    class ServerRejected(
        message: String,
        val statusCode: Int?,
        val postgresErrorCode: String?,
        cause: Throwable? = null
    ) : AppError(message, cause)

    /** Catch-all for unexpected / unclassifiable errors. */
    class Unknown(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Operation blocked because data collection is currently running. */
    class CollectionRunning(message: String) : AppError(message)
}
