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
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.resources.Resource
import kotlinx.datetime.Clock
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
    suspend fun writeDraftMessage(body: String, subject: String, to: List<ConversationUser>, cc: List<ConversationUser>, cci: List<ConversationUser>): Message {
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

        val messageId = Json.decodeFromString<JsonObject>(json)["id"]!!.jsonPrimitive.content

        return Message(
            id = messageId,
            subject = subject,
            from = client.userInfo!!.userId,
            state = "DRAFT",
            to = to.map { it.id },
            cc = cc.map { it.id },
            ccName = null,
            cci = cci.map { it.id },
            cciName = null,
            rawDisplayNames = listOf(),
            date = Clock.System.now().toEpochMilliseconds(),
            unread = false,
            response = null,
            count = null,
            hasAttachment = false,
            body = body,
            rawAttachments = listOf()
        )
    }

    /**
     * Update a draft message with its new content.
     *
     * @param draftMessage The new draft message (based on another one which should have been previously fetched)
     */
    suspend fun updateDraftMessage(draftMessage: Message) {
        client.httpClient.put(Conversation.Draft.Id(id = draftMessage.id)) {
            contentType(ContentType.Application.Json)

            setBody(draftMessage.toJson())
        }
    }

    /**
     * Send a message directly (without sending a draft).
     *
     * @param body The HTML-formatted message body
     * @param subject The message subject
     * @param to The list of primary recipients
     * @param cc The list of carbon copy recipients
     * @param cci The list of black carbon copy recipients
     * @param replyTo The message to reply to
     */
    suspend fun sendMessage(body: String, subject: String, to: List<ConversationUser>, cc: List<ConversationUser>, cci: List<ConversationUser>, replyTo: Message? = null) {
        val body = buildJsonObject {
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
        }

        if (replyTo == null)
            client.httpClient.post(Conversation.SendMessage()) {
                contentType(ContentType.Application.Json)

                setBody(body)
            }
        else
            client.httpClient.post(Conversation.SendMessageReply(replyTo = replyTo.id)) {
                contentType(ContentType.Application.Json)

                setBody(body)
            }
    }

    /**
     * Send a draft message.
     *
     * Please note that you can't reply to a message by sending a draft message.
     *
     * @param message The draft message to send
     */
    suspend fun sendDraftMessage(message: Message) {
        if (message.state != "DRAFT")
            error("Cannot send message with state (${message.state})")

        val id = message.id

        client.httpClient.post(
            Conversation.SendDraftMessage(id = id)
        ) {
            contentType(ContentType.Application.Json)

            setBody(message.toJson())
        }
    }

    /**
     * Move the given messages to trash.
     *
     * @param messages Messages to move to trash
     */
    suspend fun moveToTrash(messages: List<Message>) {
        client.httpClient.put(Conversation.Trash()) {
            contentType(ContentType.Application.Json)

            setBody(buildJsonObject {
                putJsonArray("id") {
                    messages.map { add(it.id) }
                }
            })
        }
    }

    /**
     * Restore messages from trash.
     *
     * @param messages Messages to restore
     */
    suspend fun restoreFromTrash(messages: List<Message>) {
        client.httpClient.put(Conversation.RestoreFromTrash()) {
            contentType(ContentType.Application.Json)

            setBody(buildJsonObject {
                putJsonArray("id") {
                    messages.map { add(it.id) }
                }
            })
        }
    }

    /**
     * Empty trash folder.
     */
    suspend fun emptyTrash() {
        client.httpClient.delete(Conversation.EmptyTrash())
    }

    /**
     * Upload an attachment to the given message
     *
     * @param message The message where the attachment should be uploaded
     * @param fileName The name to give to the file
     * @param data The file data to upload
     *
     * @return Attachment ID
     */
    suspend fun uploadAttachment(message: Message, fileName: String, data: ByteArray): String {
        val response = client.httpClient.post(Conversation.Message.AttachmentUpload(Conversation.Message(messageId = message.id))) {
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", data, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                    })
                },
                boundary = "EdificeKtBoundary"
            ))
        }

        val json = Json.decodeFromString<JsonObject>(response.bodyAsText())

        return json["id"]!!.jsonPrimitive.content
    }
}