package io.github.skythrew.edificekt.resources

import io.github.skythrew.edificekt.EdificeClient
import io.ktor.resources.Resource

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
}