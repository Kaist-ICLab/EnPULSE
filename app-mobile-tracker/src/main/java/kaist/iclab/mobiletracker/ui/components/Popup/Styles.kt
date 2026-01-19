package kaist.iclab.mobiletracker.ui.components.Popup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * PopupDialog style constants
 * Style definitions specific to PopupDialog component
 */
object Styles {
    // Dialog container
    val ContainerColor = Color.White
    val MaxWidth = 280.dp
    val CardPadding = 16.dp
    val CardCornerRadius = 8.dp
    
    // Title styles
    val TitleFontSize = 16.sp
    val TitleFontWeight = FontWeight.Bold
    val TitleColor = Color.Black
    val TitleBottomPadding = 12.dp
    
    // Button styles
    val ButtonDefaultWidth = 100.dp
    val ButtonDefaultHeight = 36.dp
    val ButtonTextFontSize = 13.sp
    val ButtonSpacing = 8.dp
    val ButtonCornerRadius = RoundedCornerShape(8.dp)
    val OutlinedButtonCornerRadius = RoundedCornerShape(8.dp)
    val ButtonBorderWidth = 1.dp
    val ButtonBorderColor = AppColors.BorderLight
    
    // Spacing
    val ContentButtonSpacing = 16.dp
}