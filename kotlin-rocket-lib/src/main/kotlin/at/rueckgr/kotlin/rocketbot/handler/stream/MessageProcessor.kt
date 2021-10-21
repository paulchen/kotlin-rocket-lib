package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import at.rueckgr.kotlin.rocketbot.websocket.UnsubscribeMessage
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils
import java.util.*

class MessageProcessor : Logging {
    companion object {
        val instance = MessageProcessor()
    }

    @Suppress("UNCHECKED_CAST")
    fun handleStreamMessageItem(roomMessageHandler: RoomMessageHandler, configuration: BotConfiguration, messageNode: JsonNode): List<Any> {
        val message = messageNode.get("msg").textValue()
        val roomId = messageNode.get("rid").textValue()

        val t = messageNode.get("t") ?: ""
        if (t == "ru" && message == configuration.username) {
            return listOf(UnsubscribeMessage(id = "subscribe-$roomId"))
        }

        val i = messageNode.get("bot")?.get("i")?.textValue() ?: ""
        if (StringUtils.isNotBlank(i)) {
            logger().debug("Message comes from self-declared bot, ignoring")
            return emptyList()
        }

        val username = messageNode.get("u")?.get("username")?.textValue() ?: ""
        return handleUserMessage(roomMessageHandler, configuration, roomId, username, message.trim())
    }

    private fun handleUserMessage(roomMessageHandler: RoomMessageHandler, configuration: BotConfiguration, roomId: String, username: String, message: String): List<SendMessageMessage> {
        if (username == configuration.username) {
            logger().debug("Message comes from myself, ignoring")
            return emptyList()
        }

        return roomMessageHandler
            .handle(username, message)
            .map {
                val id = UUID.randomUUID().toString()
                val botTag = mapOf("i" to configuration.host)
                SendMessageMessage(id = id, params = listOf(mapOf("_id" to id, "rid" to roomId, "msg" to it, "bot" to botTag)))
            }
    }
}
