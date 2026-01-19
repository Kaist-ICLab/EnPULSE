package kaist.iclab.wearabletracker.helpers

/**
 * Result of a permission check operation.
 */
sealed class PermissionCheckResult {
    /**
     * Permission is granted - operation can proceed.
     */
    object Granted : PermissionCheckResult()

    /**
     * Permission was requested - user needs to grant it.
     * Operation should wait for user to grant permission and try again.
     */
    object Requested : PermissionCheckResult()

    /**
     * Permission is permanently denied - user needs to enable it in settings.
     * Show permanent denial dialog.
     */
    object PermanentlyDenied : PermissionCheckResult()
}

