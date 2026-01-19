package kaist.iclab.mobiletracker.ui.screens.SensorDetailScreen

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.ui.theme.Dimens

/**
 * Sensor Detail screen style constants
 */
object Styles {
    // Layout
    val SCREEN_HORIZONTAL_PADDING = Dimens.ScreenHorizontalPadding
    val SECTION_SPACING = Dimens.SpacingTiny
    val ITEM_SPACING = Dimens.SpacingSmall
    val BOTTOM_SPACER_HEIGHT = Dimens.SpacingSmall
    
    // Header
    val HEADER_HORIZONTAL_PADDING = Dimens.SpacingTiny
    val HEADER_VERTICAL_PADDING = Dimens.SpacingSmall
    val HEADER_TITLE_FONT_SIZE = Dimens.FontSizeTitle
    val WATCH_BADGE_SIZE = Dimens.IconSizeMedium
    
    // Card
    val CARD_CORNER_RADIUS = Dimens.CornerRadiusMedium // 8dp vs 10dp before (Standardizing)
    val CARD_PADDING = Dimens.SpacingMedium // 12dp vs 14dp before (Standardizing)
    val CARD_TITLE_FONT_SIZE = Dimens.FontSizeSubtitle
    
    // Summary
    val SUMMARY_CONTENT_SPACING = Dimens.SpacingSmall
    val SUMMARY_ROW_VERTICAL_PADDING = Dimens.SpacingTiny
    val SUMMARY_LABEL_FONT_SIZE = 13.sp // Custom for now
    val SUMMARY_VALUE_FONT_SIZE = 13.sp
    
    // Section
    val SECTION_TITLE_FONT_SIZE = Dimens.FontSizeBody
    val SECTION_TITLE_VERTICAL_PADDING = 0.dp
    
    // Filter Row
    val FILTER_ROW_VERTICAL_PADDING = 0.dp
    val FILTER_BUTTON_SPACING = Dimens.SpacingSmall
    val FILTER_BUTTON_CORNER_RADIUS = Dimens.CornerRadiusMedium
    val FILTER_BUTTON_HORIZONTAL_PADDING = Dimens.SpacingMedium
    val FILTER_BUTTON_VERTICAL_PADDING = Dimens.SpacingSmall
    val FILTER_ICON_SIZE = Dimens.IconSizeSmall
    val FILTER_ICON_TEXT_SPACING = Dimens.ButtonCornerRadiusSmall
    val FILTER_BUTTON_FONT_SIZE = 13.sp
    
    // Record Card
    val RECORD_CARD_PADDING = Dimens.SpacingMedium
    val RECORD_TIMESTAMP_FONT_SIZE = 13.sp
    val RECORD_CONTENT_SPACING = Dimens.ButtonCornerRadiusSmall
    val DELETE_BUTTON_SIZE = Dimens.IconSizeLarge
    val DELETE_ICON_SIZE = Dimens.IconSizeDelete
    
    // Field Row
    val FIELD_ROW_VERTICAL_PADDING = Dimens.SpacingMicro
    val FIELD_LABEL_FONT_SIZE = Dimens.FontSizeSmall
    val FIELD_VALUE_FONT_SIZE = Dimens.FontSizeSmall
    val FIELD_LABEL_WIDTH = Dimens.WidthLabel
    
    // Empty State
    val EMPTY_STATE_VERTICAL_PADDING = Dimens.SpacingDouble
    
    // Loading
    val LOADING_INDICATOR_PADDING = Dimens.SpacingLarge
    val LOADING_INDICATOR_SIZE = Dimens.IconSizeStandard
    
    // Button
    val SMALL_BUTTON_HEIGHT = Dimens.ButtonHeightSmall
    val SMALL_BUTTON_CORNER_RADIUS = Dimens.ButtonCornerRadiusSmall
    val SMALL_BUTTON_PADDING_HORIZONTAL = Dimens.SpacingMedium
    val SMALL_BUTTON_PADDING_VERTICAL = 0.dp
    val SMALL_BUTTON_FONT_SIZE = Dimens.FontSizeSmall
}
