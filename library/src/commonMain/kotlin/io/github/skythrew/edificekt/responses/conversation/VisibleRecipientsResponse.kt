package io.github.skythrew.edificekt.responses.conversation

import io.github.skythrew.edificekt.models.conversation.ConversationGroup
import io.github.skythrew.edificekt.models.conversation.ConversationUser
import kotlinx.serialization.Serializable

@Serializable
data class VisibleRecipientsResponse(
    val groups: List<ConversationGroup>,
    val users: List<ConversationUser>
)
