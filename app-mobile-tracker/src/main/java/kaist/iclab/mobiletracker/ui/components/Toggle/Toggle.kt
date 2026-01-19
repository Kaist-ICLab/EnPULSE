package kaist.iclab.mobiletracker.ui.components.Toggle

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Reusable toggle/switch component with consistent styling across the app
 *
 * @param checked Whether the toggle is checked/on
 * @param onCheckedChange Callback when the toggle state changes
 * @param modifier Modifier to be applied to the switch
 * @param enabled Whether the toggle is enabled (default: true)
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = false
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.scale(Styles.SWITCH_SCALE),
        colors = switchColors()
    )
}

/**
 * Global switch colors configuration
 * Used consistently across all switches/toggles in the app
 *
 * Color differentiation:
 * - Enabled + Checked: Primary blue (interactive, active)
 * - Enabled + Unchecked: Dark gray (interactive, inactive)
 * - Disabled + Checked: Primary blue with reduced opacity (non-interactive, active state)
 * - Disabled + Unchecked: Light gray (non-interactive, inactive state - more faded than enabled unchecked)
 */
@Composable
private fun switchColors() = SwitchDefaults.colors(
    // Enabled states
    checkedThumbColor = AppColors.White,
    checkedTrackColor = AppColors.PrimaryColor,
    uncheckedThumbColor = AppColors.White,
    uncheckedTrackColor = AppColors.SwitchOff, // Dark gray when enabled but off

    // Border colors
    checkedBorderColor = Color.Transparent,
    uncheckedBorderColor = Color.Transparent,

    // Disabled states - use lighter/more faded colors to show non-interactive state
    disabledCheckedThumbColor = AppColors.White,
    disabledCheckedTrackColor = AppColors.PrimaryColor.copy(alpha = 0.5f), // Faded primary when disabled and checked
    disabledUncheckedThumbColor = AppColors.White,
    disabledUncheckedTrackColor = AppColors.SwitchDisabled // Light gray when disabled and unchecked (more faded)
)

