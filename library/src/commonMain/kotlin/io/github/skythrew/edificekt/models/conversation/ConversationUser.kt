package io.github.skythrew.edificekt.models.conversation

import kotlinx.serialization.Serializable

@Serializable
data class ConversationUser(
    val id: String,
    val displayName: String,
    val groupDisplayName: String?,
    val profile: String,
    val structureName: String?
)
