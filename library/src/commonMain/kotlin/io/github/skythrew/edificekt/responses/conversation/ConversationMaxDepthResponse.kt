package io.github.skythrew.edificekt.responses.conversation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMaxDepthResponse(
    @SerialName("max-depth") val maxDepth: UInt
)
