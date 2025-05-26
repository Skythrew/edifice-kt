package io.github.skythrew.edificekt.managers

import io.github.skythrew.edificekt.EdificeClient
import io.github.skythrew.edificekt.models.conversation.ConversationFolder
import io.github.skythrew.edificekt.models.conversation.ConversationUser
import io.github.skythrew.edificekt.models.conversation.Message
import io.github.skythrew.edificekt.models.conversation.MessageAttachment
import io.github.skythrew.edificekt.resources.Conversation
import io.github.skythrew.edificekt.responses.conversation.ConversationMaxDepthResponse
import io.github.skythrew.edificekt.responses.conversation.VisibleRecipientsResponse
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class ConversationManager (
    private val client: EdificeClient
) {
    /**
     * Get the different available user-created conversation folders.
     */
    suspend fun getFolders(): List<ConversationFolder> = client.httpClient.get(Conversation.UserFoldersList()).body()

    /**
     * Get the message list from a specified folder.
     *
     * @param folder Folder name (usually INBOX or a custom one)
     */
    suspend fun getFolderMessages(folder: String): List<Message> = client.httpClient.get(Conversation.FolderMessages(folder = folder)).body()

    /**
     * Get the full content of a conversation message.
     *
     * @param id Message id
     */
    suspend fun getMessage(id: String): Message = client.httpClient.get(Conversation.Message(messageId = id)).body()

    /**
     * Get the content of a message attachment.
     *
     * @param message Parent message
     * @param attachment The attachment to fetch
     */
    suspend fun getMessageAttachment(attachment: MessageAttachment) =
        client.httpClient.get(Conversation.Message.Attachment(
            Conversation.Message(messageId = attachment.message.id) , attachmentId = attachment.id)).bodyAsBytes()

    /**
     * Set the unread status of the given messages.
     *
     * @param messages Messages to be read/unread
     * @param read Read status
     */
    suspend fun setMessagesReadStatus(messages: List<Message>, read: Boolean) =
        client.httpClient.post(Conversation.MessageToggleUnread()) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("id") {
                    messages.map { add(it.id) }
                }

                put("unread", !read)
            })
        }

    /**
     * Get the possible recipients for a message.
     *
     * @param search A search pattern for the recipient's name
     */
    suspend fun getVisibleRecipients(search: String = ""): VisibleRecipientsResponse = client.httpClient.get(Conversation.VisibleRecipients(search = search)).body()

    /**
     * Get the maximum conversation depth.
     */
    suspend fun getMaxDepth(): UInt = (client.httpClient.get(Conversation.MaxDepth()).body() as ConversationMaxDepthResponse).maxDepth

    /**
     * Write a draft message.
     *
     * @param body The HTML-formatted message body
     * @param subject The message subject
     * @param to The list of primary recipients
     * @param cc The list of carbon copy recipients
     * @param cci The list of black carbon copy recipients
     *
     * @return The new draft message ID
     */
    suspend fun writeDraftMessage(body: String, subject: String, to: List<ConversationUser>, cc: List<ConversationUser>, cci: List<ConversationUser>): String {
        val json = client.httpClient.post(Conversation.Draft()) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("body", body)

                put("subject", subject)

                putJsonArray("to") {
                    to.map { add(it.id) }
                }

                putJsonArray("cc") {
                    cc.map { add(it.id) }
                }

                putJsonArray("cci") {
                    cci.map { add(it.id) }
                }
            })
        }.bodyAsText()

        return Json.decodeFromString<JsonObject>(json)["id"]!!.jsonPrimitive.content
    }

    /**
     * Update a draft message with its new content.
     *
     * @param draftMessage The new draft message (based on another one which should have been previously fetched)
     */
    suspend fun updateDraftMessage(draftMessage: Message) {
        client.httpClient.put(Conversation.Draft.Id(id = draftMessage.id)) {
            contentType(ContentType.Application.Json)

            setBody(buildJsonObject {
                put("body", draftMessage.body)
                put("subject", draftMessage.subject)

                putJsonArray("to") {
                    draftMessage.to.map { add(it) }
                }

                putJsonArray("cc") {
                    draftMessage.cc.map { add(it) }
                }

                putJsonArray("cci") {
                    draftMessage.cci.map { add(it) }
                }
            })
        }
    }
}