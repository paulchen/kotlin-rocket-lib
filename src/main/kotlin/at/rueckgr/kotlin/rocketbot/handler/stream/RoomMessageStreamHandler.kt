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

        return args
            .filter { !isIgnoredRoom(it) }
            .map { handleStreamMessageItem(it) }
    }

    private fun getRoomName(item: JsonNode): String? {
        val roomId = item.get("rid").textValue()

        return Bot.subscriptionService.getChannelNameById(roomId)
    }

    private fun isIgnoredRoom(item: JsonNode): Boolean {
        val roomName = getRoomName(item)
        if (roomName != null && botConfiguration.ignoredChannels.contains(roomName)) {
            logger().info("Message comes from ignored channel {}, ignoring", roomName)
            return true
        }
        return false
    }

    private fun handleStreamMessageItem(messageNode: JsonNode): List<SendMessageMessage> {
        val messageText = messageNode.get("msg").textValue().trim()
        val parentMessageId = messageNode.get("tmid")?.textValue()?.trim()
        val roomId = messageNode.get("rid").textValue()
        val roomName = getRoomName(messageNode)
        val timestamp = messageNode.get("ts")?.get("\$date")?.asLong()

        if (isOldMessage(timestamp, roomId)) {
            return emptyList()
        }

        val i = messageNode.get("bot")?.get("i")?.textValue() ?: ""
        val botMessage = StringUtils.isNotBlank(i)
        if (botMessage) {
            logger().debug("Message comes from self-declared bot")
        }

        val username = messageNode.get("u")?.get("username")?.textValue() ?: ""
        val userId = messageNode.get("u")?.get("_id")?.textValue() ?: ""

        val channelType = Bot.subscriptionService.getRoomType(roomId) ?: EventHandler.ChannelType.OTHER
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
            MessageHelper.instance.createSendMessage(roomId, it.message, parentMessageId, it.emoji, it.username)
        }
    }

    private fun isOldMessage(timestamp: Long?, roomId: String): Boolean {
        if (timestamp != null) {
            // There may be multiple Websocket events for the same message in a very short timeframe
            // e.g. in case some other bot automatically adds multiple reactions to the most recent message.
            // To avoid the message being processed multiple times, we need to synchronize here.
            synchronized(this) {
                val newestTimestampSeen = Bot.subscriptionService.getNewestTimestampSeen(roomId)
                if (newestTimestampSeen != null && timestamp <= newestTimestampSeen) {
                    logger().debug("Timestamp of message ({}) is not newer than newest timestamp seen ({}), ignoring", timestamp, newestTimestampSeen)
                    return true
                }
                logger().debug("Updating newest timestamp seen from {} to {}", newestTimestampSeen, timestamp)
                Bot.subscriptionService.updateNewestTimestampSeen(roomId, timestamp)
            }
        }

        return false
    }
}
