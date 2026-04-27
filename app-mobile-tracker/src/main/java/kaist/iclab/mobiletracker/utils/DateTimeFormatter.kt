package kaist.iclab.mobiletracker.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter

/**
 * Utility object for formatting timestamps
 */
object DateTimeFormatter {
    /**
     * Format Unix timestamp (milliseconds) to "YYYY-MM-DD HH:mm:ss" format
     *
     * @param timestampMillis Unix timestamp in milliseconds
     * @return Formatted timestamp string in "YYYY-MM-DD HH:mm:ss" format (UTC timezone)
     */
    fun formatTimestamp(timestampMillis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date(timestampMillis))
    }

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
     * Get current time formatted as "MM-DD HH:mm:ss"
     *
     * @return Formatted current time string in "MM-DD HH:mm:ss" format (local timezone)
     */
    fun getCurrentTimeFormatted(): String {
        return formatTimestampShort(System.currentTimeMillis())
    }

    /**
     * Parse timestamp string from "YYYY-MM-DD HH:mm:ss" format back to Unix timestamp (milliseconds)
     *
     * @param timestampString Timestamp string in "YYYY-MM-DD HH:mm:ss" format (UTC timezone)
     * @return Unix timestamp in milliseconds, or null if parsing fails
     */
    fun parseTimestamp(timestampString: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            dateFormat.parse(timestampString)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
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

