package kaist.iclab.mobiletracker.data.sensors

import kotlinx.serialization.Serializable

/**
 * Supabase data class representing message data from the phone device.
 *
 * @property id Unique identifier for the message entry (bigint primary key).
 * @property sessionId Session identifier for grouping related messages.
 * @property senderType Type of sender (e.g., "user", "system").
 * @property uuid Unique identifier for the message (UUID format).
 * @property messageType Type of message (e.g., "text", "image", "notification").
 * @property title Title of the message.
 * @property content Content/body of the message.
 * @property isRead Whether the message has been read.
 */
@Serializable
data class MessageData(
    val id: Long? = null,
    val sessionId: Long,
    val senderType: String,
    val uuid: String? = null,
    val messageType: String,
    val title: String,
    val content: String,
    val isRead: Boolean
)

