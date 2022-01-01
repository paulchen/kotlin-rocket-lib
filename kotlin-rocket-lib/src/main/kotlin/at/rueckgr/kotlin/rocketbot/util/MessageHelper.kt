package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import org.apache.commons.lang3.StringUtils
import java.util.*

class MessageHelper {
    companion object {
        val instance = MessageHelper()
    }

    fun createSendMessage(roomId: String, message: String, botId: String, emoji: String = ""): SendMessageMessage {
        val id = UUID.randomUUID().toString()
        val botTag = mapOf("i" to botId)
        val params = mutableMapOf("_id" to id, "rid" to roomId, "msg" to message, "bot" to botTag)
        if (StringUtils.isNotBlank(emoji)) {
            params["emoji"] = emoji
        }
        return SendMessageMessage(id = id, params = listOf(params))
    }
}
