package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.exception.LoginException
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.RestApiClient
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.RoomsGetMessage
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@Suppress("unused")
class ResultMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration), Logging {
    override fun getHandledMessage() = "result"

    override fun handleMessage(data: JsonNode, timestamp: Long) = when (val id = data.get("id")?.textValue()) {
        "login-initial" -> handleLoginInitial(data)
        "get-rooms-initial" -> handleGetRoomsResult(data)
        else -> {
            logger().debug("Ignoring message with id {}", id)
            emptyArray()
        }
    }

    private fun handleLoginInitial(data: JsonNode): Array<Any> {
        if (data.has("error")) {
            throw LoginException(data.get("error")?.get("message")?.textValue() ?: "Unknown error")
        }
        val userId = data.get("result").get("id").textValue()

        Bot.userId = userId
        Bot.authToken = data.get("result").get("token").textValue()

        return arrayOf(
            RoomsGetMessage(id = "get-rooms-initial"),
            SubscribeMessage(id = "subscribe-stream-notify-user", name = "stream-notify-user", params = arrayOf("$userId/rooms-changed", false))
        )
    }

    private fun handleGetRoomsResult(data: JsonNode): Array<Any> {
        val rooms = data.get("result")
        rooms
            .filter { it.get("t").textValue() == "c" }
            .forEach { Bot.knownChannelNamesToIds[it.get("name").textValue()] = it.get("_id").textValue() }

        RestApiClient(this.botConfiguration).updateStatus()

        return emptyArray()
    }
}
