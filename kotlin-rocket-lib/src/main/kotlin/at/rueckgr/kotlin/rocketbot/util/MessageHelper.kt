package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import java.util.*

class MessageHelper {
    companion object {
        val instance = MessageHelper()
    }

    fun createSendMessage(roomId: String, message: String, botId: String): SendMessageMessage {
        val id = UUID.randomUUID().toString()
        val botTag = mapOf("i" to botId)
        return SendMessageMessage(id = id, params = listOf(mapOf("_id" to id, "rid" to roomId, "msg" to message, "bot" to botTag)))
    }
}
