package kaist.iclab.mobiletracker.ui.components.LoadingOverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Loading overlay component
 * Displays a semi-transparent overlay with a white box containing a blue spinner
 * 
 * @param isLoading Whether the loading state is active
 * @param showOverlay Whether to show the overlay (default: true)
 * @param blockNavigation Whether to block navigation by covering the entire screen including navbar (default: true)
 * @param modifier Modifier for the overlay
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    showOverlay: Boolean = true,
    blockNavigation: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isLoading && showOverlay) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // White box in center with elevation
            Surface(
                modifier = Modifier.size(Styles.WHITE_BOX_SIZE),
                shape = RoundedCornerShape(Styles.WHITE_BOX_CORNER_RADIUS),
                color = AppColors.White,
                shadowElevation = Styles.WHITE_BOX_ELEVATION
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Styles.WHITE_BOX_PADDING),
                    contentAlignment = Alignment.Center
                ) {
                    // Blue spinner
                    CircularProgressIndicator(
                        modifier = Modifier.size(Styles.SPINNER_SIZE),
                        color = AppColors.PrimaryColor,
                        strokeWidth = Styles.SPINNER_STROKE_WIDTH
                    )
                }
            }
        }
    }
}

/**
 * Loading overlay component using LoadingState
 * 
 * @param loadingState The loading state containing isLoading, showOverlay, and blockNavigation
 * @param modifier Modifier for the overlay
 */
@Composable
fun LoadingOverlay(
    loadingState: LoadingState,
    modifier: Modifier = Modifier
) {
    LoadingOverlay(
        isLoading = loadingState.isLoading,
        showOverlay = loadingState.showOverlay,
        blockNavigation = loadingState.blockNavigation,
        modifier = modifier
    )
}

