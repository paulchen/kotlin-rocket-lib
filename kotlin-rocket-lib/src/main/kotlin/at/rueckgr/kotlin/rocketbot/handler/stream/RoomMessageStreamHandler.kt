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

class RoomMessageStreamHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractStreamHandler(roomMessageHandler, botConfiguration), Logging {
    override fun getHandledStream() = "stream-room-messages"

    @Suppress("UNCHECKED_CAST")
    override fun handleStreamMessage(configuration: BotConfiguration, data: JsonNode): List<List<Any>> {
        val args = data.get("fields")?.get("args") ?: emptyList()

        return args.map { handleStreamMessageItem(configuration, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleStreamMessageItem(configuration: BotConfiguration, it: JsonNode): List<Any> {
        val message = it.get("msg").textValue()
        val roomId = it.get("rid").textValue()

        val t = it.get("t") ?: ""
        if (t == "ru" && message == configuration.username) {
            return listOf(UnsubscribeMessage(id = "subscribe-$roomId"))
        }

        val i = it.get("bot")?.get("i")?.textValue() ?: ""
        if (StringUtils.isNotBlank(i)) {
            logger().debug("Message comes from self-declared bot, ignoring")
            return emptyList()
        }

        val username = it.get("u")?.get("username")?.textValue() ?: ""
        return handleUserMessage(configuration.username, roomId, username, message.trim())
    }

    private fun handleUserMessage(ownUsername: String, roomId: String, username: String, message: String): List<SendMessageMessage> {
        if (username == ownUsername) {
            logger().debug("Message comes from myself, ignoring")
            return emptyList()
        }

        return roomMessageHandler
            .handle(username, message)
            .map {
                val id = UUID.randomUUID().toString()
                val botTag = mapOf("i" to botConfiguration.host)
                SendMessageMessage(id = id, params = listOf(mapOf("_id" to id, "rid" to roomId, "msg" to it, "bot" to botTag)))
            }
    }
}
