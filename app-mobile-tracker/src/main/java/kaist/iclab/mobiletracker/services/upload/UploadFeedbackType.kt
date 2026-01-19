package kaist.iclab.mobiletracker.services.upload

/**
 * Type of feedback to show after upload operation
 */
enum class UploadFeedbackType {
    /**
     * Show toast notification (for manual uploads)
     */
    TOAST,

    /**
     * Show system notification (for automatic uploads)
     */
    NOTIFICATION
}
