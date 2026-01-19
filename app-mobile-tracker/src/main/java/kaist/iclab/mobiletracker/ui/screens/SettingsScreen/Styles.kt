package kaist.iclab.mobiletracker.ui.screens.SettingsScreen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Common settings screen style constants
 * Shared styles used across all settings sub-screens
 */
object Styles {
    // Header (common across all settings screens)
    val HEADER_HEIGHT = 56.dp
    val HEADER_HORIZONTAL_PADDING = 8.dp
    val TITLE_FONT_SIZE = 22.sp

    // Card (common across card-based settings screens)
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_HORIZONTAL_PADDING = 16.dp
    val CARD_VERTICAL_PADDING = 4.dp
    val CARD_CONTAINER_HORIZONTAL_PADDING = 10.dp
    val CARD_SHAPE = RoundedCornerShape(CARD_CORNER_RADIUS)
    val CARD_SPACING = 10.dp

    // Container shape that wraps all cards
    val CONTAINER_SHAPE = RoundedCornerShape(CARD_CORNER_RADIUS)

    // Individual card shape (no rounding, since container handles it)
    val INDIVIDUAL_CARD_SHAPE = RoundedCornerShape(0.dp)

    // Screen Description (below main title - common in PhoneSensor, Permission, Language)
    val SCREEN_DESCRIPTION_FONT_SIZE = 14.sp
    val SCREEN_DESCRIPTION_HORIZONTAL_PADDING = 30.dp
    val SCREEN_DESCRIPTION_BOTTOM_PADDING = 12.dp

    // Typography (common across settings screens)
    val TEXT_FONT_SIZE = 15.sp
    val TEXT_LINE_HEIGHT = 18.sp
    val TEXT_TOP_PADDING = 3.dp

    // Description (used in main SettingsScreen and EnableTrackerCard)
    val DESCRIPTION_FONT_SIZE = 12.sp
    val DESCRIPTION_LINE_HEIGHT = 15.sp
    val DESCRIPTION_TOP_PADDING = 4.dp

    // Card Description (below card title)
    val CARD_DESCRIPTION_FONT_SIZE = 12.sp
    val CARD_DESCRIPTION_LINE_HEIGHT = 15.sp
    val CARD_DESCRIPTION_TOP_PADDING = 4.dp
    val CARD_DESCRIPTION_BOTTOM_PADDING = 3.dp

    // Icon and Spacing (common across settings screens)
    val ICON_SIZE = 24.dp
    val ICON_SPACER_WIDTH = 15.dp
    val SPACER_WIDTH = 10.dp

    // Divider
    val DIVIDER_WIDTH_RATIO = 0.9f

    // Menu item (used in main SettingsScreen)
    val MENU_ITEM_HORIZONTAL_PADDING = 16.dp
    val MENU_ITEM_VERTICAL_PADDING = 16.dp

    // Enable Tracker Card (used in main SettingsScreen)
    val ENABLE_TRACKER_VERTICAL_PADDING = 12.dp

    // LazyColumn (used in main SettingsScreen)
    val LAZY_COLUMN_TOP_PADDING = 8.dp

    // Header for main SettingsScreen (slightly different)
    val HEADER_START_PADDING = 16.dp
    val HEADER_END_PADDING = 8.dp
    val HEADER_FONT_SIZE = 22.sp
}
