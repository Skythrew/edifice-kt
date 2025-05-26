package io.github.skythrew.edificekt.resources

import io.ktor.resources.*

@Resource("/conversation")
class Conversation {
    @Resource("userfolders/list")
    class UserFoldersList(val parent: Conversation = Conversation())

    @Resource("list/{folder}")
    class FolderMessages(val parent: Conversation = Conversation(), val folder: String)

    @Resource("message/{messageId}")
    class Message(val parent: Conversation = Conversation(), val messageId: String) {
        @Resource("attachment/{attachmentId}")
        class Attachment(val parent: Message, val attachmentId: String)
    }

    @Resource("toggleUnread")
    class MessageToggleUnread(val parent: Conversation = Conversation())

    @Resource("visible")
    class VisibleRecipients(val parent: Conversation = Conversation(), val search: String)

    @Resource("max-depth")
    class MaxDepth(val parent: Conversation = Conversation())
}