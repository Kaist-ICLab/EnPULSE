package kaist.iclab.mobiletracker.ui.components.loading

/**
 * Represents the loading overlay state
 */
data class LoadingState(
    val isLoading: Boolean = false,
    val showOverlay: Boolean = true,
    val blockNavigation: Boolean = true
)

