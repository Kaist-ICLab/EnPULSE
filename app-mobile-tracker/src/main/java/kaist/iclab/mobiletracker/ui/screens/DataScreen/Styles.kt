package kaist.iclab.mobiletracker.ui.screens.DataScreen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.ui.theme.Dimens

/**
 * Data screen style constants
 */
object Styles {
    // Layout
    val SCREEN_HORIZONTAL_PADDING = Dimens.ScreenHorizontalPadding
    val TOP_SPACER_HEIGHT = Dimens.SpacingLarge
    val BOTTOM_SPACER_HEIGHT = Dimens.SpacingSmall
    val SECTION_SPACING = Dimens.SpacingLarge
    val ITEM_SPACING = Dimens.SpacingSmall
    
    // Header
    val TITLE_FONT_SIZE = Dimens.FontSizeLargeHeader
    val DESCRIPTION_FONT_SIZE = Dimens.FontSizeBody
    val SUBTITLE_FONT_SIZE = Dimens.FontSizeBody
    val SUBTITLE_TOP_PADDING = Dimens.SpacingTiny
    
    // Card
    val CARD_SHAPE = RoundedCornerShape(Dimens.CornerRadiusMedium)
    val CARD_ELEVATION = Dimens.ElevationNone
    val CARD_PADDING = Dimens.SpacingSmall
    
    // Icon
    val ICON_CONTAINER_SIZE = Dimens.IconSizeContainer
    val ICON_SIZE = Dimens.IconSizeStandard
    val ICON_CORNER_RADIUS = Dimens.CornerRadiusMedium
    val ICON_TEXT_SPACING = Dimens.SpacingMedium
    
    // Text
    val SENSOR_NAME_FONT_SIZE = Dimens.FontSizeSubtitle
    val LAST_RECORDED_FONT_SIZE = Dimens.FontSizeSmall
    val RECORD_COUNT_FONT_SIZE = Dimens.FontSizeSmall
    
    // Badge
    val BADGE_SPACING = Dimens.SpacingTiny
    val BADGE_SIZE = Dimens.IconSizeTiny
    
    // Chevron
    val CHEVRON_SPACING = Dimens.SpacingTiny
    val CHEVRON_SIZE = Dimens.IconSizeSmall
}
