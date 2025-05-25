package io.github.skythrew.edificekt.models.conversation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationFolder(
    val id: String,
    @SerialName("parent_id") val parentId: String?,
    val depth: UShort,
    val name: String,
    val nbUnread: UShort,
    @SerialName("skip_uniq") val skipUnique: Boolean?,
    val trashed: Boolean
)
