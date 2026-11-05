package kaist.iclab.mobiletracker.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter

/**
 * Utility object for formatting timestamps
 */
object DateTimeFormatter {

    /**
     * Format Unix timestamp (milliseconds) to "MM-DD HH:mm:ss" format
     *
     * @param timestampMillis Unix timestamp in milliseconds
     * @return Formatted timestamp string in "MM-DD HH:mm:ss" format (local timezone)
     */
    fun formatTimestampShort(timestampMillis: Long): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timestampMillis))
    }

    /**
     * Format Unix timestamp (milliseconds) to ISO 8601 Offset Date Time string.
     * Used for Supabase (PostgreSQL) 'timestamptz' columns.
     *
     * @param timestampMillis Unix timestamp in milliseconds
     * @return ISO 8601 formatted string (e.g., "2024-04-24T17:00:00Z")
     */
    fun formatToIsoOffset(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atOffset(ZoneOffset.UTC)
            .format(JavaDateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    /**
     * Safely format a nullable millisecond string to ISO 8601 Offset Date Time string.
     *
     * @param millisStr Unix timestamp in milliseconds as a string (can be null)
     * @return ISO 8601 formatted string or null if input is null or invalid
     */
    fun formatToIsoOffset(millisStr: String?): String? {
        return millisStr?.toLongOrNull()?.let { formatToIsoOffset(it) }
    }
}

