package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.exception.LoginException
import at.rueckgr.kotlin.rocketbot.websocket.RoomsGetMessage
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode

class ResultMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration) {
    override fun getHandledMessage() = "result"

    override fun handleMessage(configuration: BotConfiguration, data: JsonNode, timestamp: Long) = when (data.get("id")?.textValue()) {
        "login-initial" -> handleLoginInitial(data)
        "get-rooms-initial" -> handleGetRoomsResult(configuration.ignoredChannels, data)
        else -> emptyArray()
    }

    private fun handleLoginInitial(data: JsonNode): Array<Any> {
        if (data.has("error")) {
            throw LoginException(data.get("error")?.get("message")?.textValue() ?: "Unknown error")
        }
        val userId = data.get("result").get("id")
        return arrayOf(
            RoomsGetMessage(id = "get-rooms-initial"),
            SubscribeMessage(id = "subscribe-stream-notify-user", name = "stream-notify-user", params = arrayOf("$userId/rooms-changed", false))
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleGetRoomsResult(ignoredChannels: List<String>, data: JsonNode): Array<Any> {
        val rooms = data.get("result")
        return rooms
            .filter { it.get("t").textValue() == "c" }
            .filter { !ignoredChannels.contains(it.get("name").textValue()) }
            .map {
                val id = it.get("_id").textValue()
                SubscribeMessage(id = "subscribe-$id", name = "stream-room-messages", params = arrayOf(id, false))
            }
            .toTypedArray()
    }
}
