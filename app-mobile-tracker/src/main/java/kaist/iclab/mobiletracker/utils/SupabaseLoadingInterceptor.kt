package kaist.iclab.mobiletracker.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interceptor for tracking active Supabase operations to show/hide loading overlay.
 * Thread-safe counter that tracks the number of active Supabase operations.
 */
object SupabaseLoadingInterceptor {
    private var activeOperations = 0
    private val mutex = Mutex()

    /**
     * Callback to be set from MainScreen to update loading state
     */
    var onLoadingStateChanged: ((Boolean) -> Unit)? = null

    /**
     * Increment active operations counter and notify listener
     * Call this at the start of a Supabase operation
     */
    suspend fun startOperation() {
        mutex.withLock {
            activeOperations++
            if (activeOperations == 1) {
                // First operation started - show loading
                onLoadingStateChanged?.invoke(true)
            }
        }
    }

    /**
     * Decrement active operations counter and notify listener
     * Call this at the end of a Supabase operation (in finally block)
     */
    suspend fun endOperation() {
        mutex.withLock {
            if (activeOperations > 0) {
                activeOperations--
                if (activeOperations == 0) {
                    // All operations completed - hide loading
                    onLoadingStateChanged?.invoke(false)
                }
            }
        }
    }

    /**
     * Execute a Supabase operation with automatic loading state management
     * @param operation The suspend function to execute
     * @return The result of the operation
     */
    suspend fun <T> withLoading(operation: suspend () -> T): T {
        startOperation()
        return try {
            operation()
        } finally {
            endOperation()
        }
    }
}

