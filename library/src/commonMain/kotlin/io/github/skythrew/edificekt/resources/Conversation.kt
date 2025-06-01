package io.github.skythrew.edificekt.resources

import io.ktor.resources.*
import kotlinx.serialization.SerialName

@Resource("/conversation")
class Conversation {
    @Resource("userfolders/list")
    class UserFoldersList(val parent: Conversation = Conversation())

    @Resource("list/{folder}")
    class FolderMessages(val parent: Conversation = Conversation(), val folder: String)

    @Resource("message/{messageId}")
    class Message(val parent: Conversation = Conversation(), val messageId: String) {
        @Resource("attachment")
        class AttachmentUpload(val parent: Message)

        @Resource("attachment/{attachmentId}")
        class Attachment(val parent: Message, val attachmentId: String)
    }

    @Resource("toggleUnread")
    class MessageToggleUnread(val parent: Conversation = Conversation())

    @Resource("visible")
    class VisibleRecipients(val parent: Conversation = Conversation(), val search: String)

    @Resource("max-depth")
    class MaxDepth(val parent: Conversation = Conversation())

    @Resource("draft")
    class Draft(val parent: Conversation = Conversation()) {
        @Resource("{id}")
        class Id(val parent: Draft = Draft(), val id: String)
    }

    @Resource("send")
    class SendMessage(val parent: Conversation = Conversation())

    @Resource("send")
    class SendDraftMessage(val parent: Conversation = Conversation(), val id: String)

    @Resource("send")
    class SendMessageReply(val parent: Conversation = Conversation(), @SerialName("In-Reply-To") val replyTo: String)

    @Resource("trash")
    class Trash(val parent: Conversation = Conversation())

    @Resource("emptyTrash")
    class EmptyTrash(val parent: Conversation = Conversation())

    @Resource("restore")
    class RestoreFromTrash(val parent: Conversation = Conversation())
}