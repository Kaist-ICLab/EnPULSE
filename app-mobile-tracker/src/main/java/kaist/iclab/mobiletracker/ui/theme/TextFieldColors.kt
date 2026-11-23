package kaist.iclab.mobiletracker.ui.theme

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

/** Outlined text field colors in the app's blue, instead of Material's default purple. */
@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.PrimaryColor,
    unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.5f),
    errorBorderColor = AppColors.ErrorColor,
    focusedLabelColor = AppColors.PrimaryColor,
    unfocusedLabelColor = AppColors.TextSecondary,
    cursorColor = AppColors.PrimaryColor
)
