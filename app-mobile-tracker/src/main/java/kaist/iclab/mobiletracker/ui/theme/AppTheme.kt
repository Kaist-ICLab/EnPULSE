package kaist.iclab.mobiletracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Maps [AppColors] onto Material's color roles, so components without explicit colors (text
 * fields, buttons, switches, spinners, selection handles) use the app's blue instead of
 * Material's default purple. The app has a light design only.
 */
private val AppColorScheme = lightColorScheme(
    primary = AppColors.PrimaryColor,
    onPrimary = AppColors.White,
    secondary = AppColors.SecondaryColor,
    onSecondary = AppColors.White,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.White,
    onSurface = AppColors.TextPrimary,
    outline = AppColors.BorderDark,
    outlineVariant = AppColors.BorderLight,
    error = AppColors.ErrorColor,
    onError = AppColors.White
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
