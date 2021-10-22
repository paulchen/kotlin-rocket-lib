package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.exception.LoginException
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode

@Suppress("unused")
class ResultMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration) {
    override fun getHandledMessage() = "result"

    override fun handleMessage(data: JsonNode, timestamp: Long) = when (data.get("id")?.textValue()) {
        "login-initial" -> handleLoginInitial(data)
        else -> emptyArray()
    }

    private fun handleLoginInitial(data: JsonNode): Array<Any> {
        if (data.has("error")) {
            throw LoginException(data.get("error")?.get("message")?.textValue() ?: "Unknown error")
        }
        val userId = data.get("result").get("id").textValue()
        return arrayOf(
            SubscribeMessage(id = "subscribe-stream-notify-user", name = "stream-notify-user", params = arrayOf("$userId/rooms-changed", false))
        )
    }
}
