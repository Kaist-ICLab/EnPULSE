package kaist.iclab.mobiletracker.data.sensors

import kotlinx.serialization.Serializable

/**
 * Supabase data class representing chat session data from the phone device.
 *
 * @property id Unique identifier for the chat session entry (bigint primary key).
 * @property uuid Unique identifier for the chat session (UUID format).
 * @property campaignId Campaign identifier associated with the chat session.
 * @property lastMessage Content of the last message in the session.
 * @property lastMessageTime Unix timestamp in milliseconds when the last message was sent.
 * @property unreadCount Number of unread messages in the session.
 */
@Serializable
data class ChatSessionData(
    val id: Long? = null,
    val uuid: String? = null,
    val campaignId: Int,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int
)

