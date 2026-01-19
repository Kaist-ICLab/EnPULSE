package kaist.iclab.mobiletracker.ui.screens.HomeScreen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home screen style constants
 */
object Styles {
    // Layout spacings
    val SCREEN_HORIZONTAL_PADDING = 16.dp
    val SCREEN_VERTICAL_SPACING = 8.dp
    val TOP_SPACER_HEIGHT = 4.dp
    val BOTTOM_SPACER_HEIGHT = 8.dp
    
    // Greeting Section
    val GREETING_TITLE_FONT_SIZE = 22.sp
    val GREETING_SUBTITLE_FONT_SIZE = 14.sp
    val GREETING_SUBTITLE_TOP_PADDING = 4.dp
    
    // Grid Spacing
    val GRID_SECTION_TITLE_FONT_SIZE = 16.sp
    val GRID_SECTION_TITLE_TOP_PADDING = 2.dp

    // Tracking Status Card
    val STATUS_CARD_CORNER_RADIUS = 16.dp
    val STATUS_CARD_SHAPE = RoundedCornerShape(STATUS_CARD_CORNER_RADIUS)
    val STATUS_CARD_ELEVATION = 0.dp
    val STATUS_CARD_PADDING = 14.dp
    val STATUS_TITLE_FONT_SIZE = 15.sp
    val STATUS_SUBTITLE_FONT_SIZE = 13.sp
    val STATUS_SUBTITLE_TOP_PADDING = 2.dp
    
    // Status Indicator (Tag)
    val INDICATOR_CORNER_RADIUS = 12.dp
    val INDICATOR_SHAPE = RoundedCornerShape(INDICATOR_CORNER_RADIUS)
    val INDICATOR_HORIZONTAL_PADDING = 10.dp
    val INDICATOR_VERTICAL_PADDING = 2.dp
    val INDICATOR_FONT_SIZE = 11.sp

    // Insight Row (New List Layout)
    val INSIGHT_ROW_VERTICAL_SPACING = 8.dp
    val INSIGHT_ROW_PADDING_HORIZONTAL = 12.dp
    val INSIGHT_ROW_PADDING_VERTICAL = 10.dp
    val INSIGHT_ROW_ICON_SIZE = 22.dp
    val INSIGHT_ROW_VALUE_FONT_SIZE = 14.sp
    val INSIGHT_ROW_LABEL_FONT_SIZE = 14.sp
    val INSIGHT_ROW_CORNER_RADIUS = 12.dp
    val INSIGHT_ROW_SHAPE = RoundedCornerShape(INSIGHT_ROW_CORNER_RADIUS)

    /**
     * Home screen color palette
     */
    object Colors {
        // Sensor Colors
        val LOCATION = Color(0xFF4285F4)
        val APP_USAGE = Color(0xFF9C27B0)
        val ACTIVITY = Color(0xFF34A853)
        val DEVICE_STATUS = Color(0xFFFBBC04)
        val NOTIFICATIONS = Color(0xFFEA4335)
        val SCREEN = Color(0xFF607D8B)
        val CONNECTIVITY = Color(0xFF00ACC1)
        val BLUETOOTH = Color(0xFF3F51B5)
        val AMBIENT_LIGHT = Color(0xFFFF9800)
        val APP_LIST_CHANGE = Color(0xFFE91E63)
        val CALL_LOG = Color(0xFF8BC34A)
        val DATA_TRAFFIC = Color(0xFF009688)
        val DEVICE_MODE = Color(0xFF795548)
        val MEDIA = Color(0xFFFF5722)
        val MESSAGE_LOG = Color(0xFFCDDC39)
        val USER_INTERACTION = Color(0xFF673AB7)
        val WIFI_SCAN = Color(0xFF00BCD4)
        
        // Watch Sensor Colors
        val WATCH_HEART_RATE = Color(0xFFE91E63)
        val WATCH_ACCELEROMETER = Color(0xFF3F51B5)
        val WATCH_EDA = Color(0xFF00BCD4)
        val WATCH_PPG = Color(0xFFFF5722)
        val WATCH_SKIN_TEMP = Color(0xFFFF9800)
        
        // Status Indicator
        val RUNNING_BG = Color(0xFFE6F4EA)
        val RUNNING_TEXT = Color(0xFF137333)
        
        val STOPPED_BG = Color(0xFFFCE8E8)
        val STOPPED_TEXT = Color(0xFFC5221F)

        val WARNING_BG = Color(0xFFFEF7E0)
        val WARNING_TEXT = Color(0xFFB06000)
    }
}
