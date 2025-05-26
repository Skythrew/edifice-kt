package io.github.skythrew.edificekt.models.conversation

import kotlinx.serialization.Serializable

@Serializable
data class ConversationGroup(
    val id: String,
    val name: String,
    val groupDisplayName: String?,
    val profile: String,
    val structureName: String?
)
