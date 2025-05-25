package io.github.skythrew.edificekt.managers

import io.github.skythrew.edificekt.EdificeClient
import io.github.skythrew.edificekt.models.conversation.ConversationFolder
import io.github.skythrew.edificekt.models.conversation.Message
import io.github.skythrew.edificekt.models.conversation.MessageAttachment
import io.github.skythrew.edificekt.resources.Conversation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
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
}