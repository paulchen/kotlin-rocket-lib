package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.MessageHelper
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils

@Suppress("unused")
class RoomMessageStreamHandler(eventHandler: EventHandler, botConfiguration: BotConfiguration)
    : AbstractStreamHandler(eventHandler, botConfiguration), Logging {
    override fun getHandledStream() = "stream-room-messages"

    @Suppress("UNCHECKED_CAST")
    override fun handleStreamMessage(data: JsonNode): List<List<Any>> {
        val args = data.get("fields")?.get("args") ?: emptyList()

        args.forEach {
            println(it)
        }
        return args
            .filter { !isIgnoredRoom(it) }
            .map { handleStreamMessageItem(it) }
    }

    private fun getRoomName(item: JsonNode): String {
        val roomId = item.get("rid").textValue()

        return Bot.knownChannelNamesToIds[roomId] ?: roomId
    }

    private fun isIgnoredRoom(item: JsonNode): Boolean {
        // TODO subscription: clean up here
        val roomName = getRoomName(item) // TODO private messages ?: return false // private messages don't have an fname
        if (botConfiguration.ignoredChannels.contains(roomName)) {
            logger().info("Message comes from ignored channel {}, ignoring", roomName)
            return true
        }
        return false
    }

    private fun handleStreamMessageItem(messageNode: JsonNode): List<SendMessageMessage> {
        // TODO subscription: code duplication?
        val messageText = messageNode.get("msg").textValue().trim()
        val roomId = messageNode.get("rid").textValue()
        val roomName = getRoomName(messageNode)

        val i = messageNode.get("bot")?.get("i")?.textValue() ?: ""
        val botMessage = StringUtils.isNotBlank(i)
        if (botMessage) {
            logger().debug("Message comes from self-declared bot")
        }

        val username = messageNode.get("u")?.get("username")?.textValue() ?: ""
        val userId = messageNode.get("u")?.get("_id")?.textValue() ?: ""

        // TODO subscription: clean this up
//        val channelType = mapChannelType(item.get("t")?.textValue())
        val channelType = EventHandler.ChannelType.CHANNEL

        val channel = EventHandler.Channel(roomId, roomName, channelType)
        val user = EventHandler.User(userId, username)
        val message = EventHandler.Message(messageText, botMessage)

        val outgoingMessages = if(username == botConfiguration.username) {
            logger().debug("Message comes from myself")
            eventHandler.handleOwnMessage(channel, user, message)
        }
        else {
            eventHandler.handleRoomMessage(channel, user, message)
        }
        return outgoingMessages.map {
            MessageHelper.instance.createSendMessage(roomId, it.message, botConfiguration.botId, it.emoji, it.username)
        }
    }

    // TODO subscription: clean this up
    private fun mapChannelType(t: String?) = when (t) {
        "c" -> EventHandler.ChannelType.CHANNEL
        "d" -> EventHandler.ChannelType.DIRECT
        else -> EventHandler.ChannelType.OTHER
    }
}
