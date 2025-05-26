package io.github.skythrew.edificekt.models.conversation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

interface MessageAttachmentInterface {
    val id: String
    val name: String
    val charset: String
    val filename: String
    val contentType: String
    val contentTransferEncoding: String
    val size: Long
}

@Serializable
data class RawMessageAttachment(
    override val id: String,
    override val name: String,
    override val charset: String,
    override val filename: String,
    override val contentType: String,
    override val contentTransferEncoding: String,
    override val size: Long,
) : MessageAttachmentInterface

@Serializable
data class MessageAttachment(
    override val id: String,
    override val name: String,
    override val charset: String,
    override val filename: String,
    override val contentType: String,
    override val contentTransferEncoding: String,
    override val size: Long,
    val message: Message
) : MessageAttachmentInterface {
    companion object {
        fun fromRaw(message: Message, rawMessageAttachment: RawMessageAttachment) = MessageAttachment(
            id = rawMessageAttachment.id,
            name = rawMessageAttachment.name,
            charset = rawMessageAttachment.charset,
            filename = rawMessageAttachment.filename,
            contentType = rawMessageAttachment.contentType,
            contentTransferEncoding = rawMessageAttachment.contentTransferEncoding,
            size = rawMessageAttachment.size,
            message = message
        )
    }
}

@Serializable
data class Message(
    val id: String,
    val subject: String?,
    val from: String,
    val state: String,
    val to: List<String>,
    val cc: List<String>,
    val ccName: List<String>?,
    val cci: List<String>,
    val cciName: List<String>?,
    @SerialName("displayNames") private val rawDisplayNames: List<JsonArray>,
    val date: Long,
    val unread: Boolean = false,
    val response: Boolean? = null,
    val count: UShort? = null,
    val hasAttachment: Boolean? = null,
    val body: String? = null,
    @SerialName("attachments") private val rawAttachments: List<RawMessageAttachment>? = null
) {
    @Transient
    val displayNames: Map<String, String> = rawDisplayNames.associate { Pair(it[0].jsonPrimitive.content, it[1].jsonPrimitive.content) }

    @Transient
    val fromName = displayNames[from]

    @Transient
    val toName = to.map { displayNames[it] }.joinToString(", ")

    @Transient
    val attachments = rawAttachments?.map { MessageAttachment.fromRaw(this, it) }

    fun toJson(): JsonObject = buildJsonObject {
        put("body", body)
        put("subject", subject)

        putJsonArray("to") {
            to.map { add(it) }
        }

        putJsonArray("cc") {
            cc.map { add(it) }
        }

        putJsonArray("cci") {
            cci.map { add(it) }
        }
    }
}
