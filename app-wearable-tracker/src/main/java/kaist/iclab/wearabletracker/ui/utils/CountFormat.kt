package kaist.iclab.wearabletracker.ui.utils

import android.annotation.SuppressLint

/**
 * Format large numbers compactly: 1234 -> "1.2K", 1234567 -> "1.2M".
 * Screen real estate on the watch is tight, so record counts are always abbreviated.
 */
@SuppressLint("DefaultLocale")
fun formatCompactCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
