package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.exception.LoginException
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.MessageHelper
import at.rueckgr.kotlin.rocketbot.util.RestApiClient
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.RoomsGetMessage
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode

@Suppress("unused")
class ResultMessageHandler(eventHandler: EventHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(eventHandler, botConfiguration), Logging {
    override fun getHandledMessage() = "result"

    override fun handleMessage(data: JsonNode) = when (val id = data.get("id")?.textValue()) {
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
            SubscribeMessage(id = "subscribe-stream-notify-user-rooms", name = "stream-notify-user", params = arrayOf("$userId/rooms-changed", false)),
            SubscribeMessage(id = "subscribe-stream-notify-user-subscriptions", name = "stream-notify-user", params = arrayOf("$userId/subscriptions-changed", false))
        )
    }

    private fun handleGetRoomsResult(data: JsonNode): Array<Any> {
        val rooms = data.get("result")
        val messages = rooms
            .filter { MessageHelper.instance.mapChannelType(it.get("t").textValue()) != EventHandler.ChannelType.OTHER }
            .mapNotNull {
                val id = it.get("_id").textValue()
                val name = it.get("name")?.textValue()
                val type = MessageHelper.instance.mapChannelType(it.get("t").textValue())
                val timestamp = it.get("lastMessage")?.get("ts")?.get("\$date")?.longValue() ?: 0L

                Bot.subscriptionService.handleSubscription(id, name, type, timestamp)
            }

        RestApiClient(botConfiguration.host).updateStatus()

        eventHandler.botInitialized()

        return messages.toTypedArray()
    }
}
