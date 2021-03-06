package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.MessageHelper
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils
import kotlin.collections.ArrayList

@Suppress("unused")
class NotifyUserStreamHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractStreamHandler(roomMessageHandler, botConfiguration), Logging {
    override fun getHandledStream() = "stream-notify-user"

    override fun handleStreamMessage(data: JsonNode): List<List<Any>> {
        return when (MessageHelper.instance.getEventName(data)) {
            "rooms-changed" -> handleRoomsChangedEvent(data)
            else -> emptyList()
        }
    }

    private fun handleRoomsChangedEvent(data: JsonNode): List<List<Any>> {
        val args: JsonNode = data.get("fields")?.get("args") ?: return emptyList()

        return when (args.get(0).textValue()) {
            "updated" -> handleMessage(args)
            else -> emptyList()
        }
    }

    private fun handleMessage(args: JsonNode): List<List<Any>> {

        val items = ArrayList<JsonNode>()
        for (i in 1 until args.size()) {
            items.add(args.get(i))
        }

        return items
            .filter { !isIgnoredRoom(it) }
            .map { handleStreamMessageItem(it) }
    }

    private fun getRoomName(item: JsonNode) = item.get("fname")?.textValue()

    private fun isIgnoredRoom(item: JsonNode): Boolean {
        val roomName = getRoomName(item) ?: return false // private messages don't have an fname
        if (botConfiguration.ignoredChannels.contains(roomName)) {
            logger().info("Message comes from ignored channel {}, ignoring", roomName)
            return true
        }
        return false
    }

    private fun handleStreamMessageItem(item: JsonNode): List<SendMessageMessage> {
        val messageNode = item.get("lastMessage")

        val messageText = messageNode.get("msg").textValue().trim()
        val roomId = messageNode.get("rid").textValue()
        val roomName = getRoomName(item)

        val i = messageNode.get("bot")?.get("i")?.textValue() ?: ""
        val botMessage = StringUtils.isNotBlank(i)
        if (botMessage) {
            logger().debug("Message comes from self-declared bot")
        }

        val username = messageNode.get("u")?.get("username")?.textValue() ?: ""
        val userId = messageNode.get("u")?.get("_id")?.textValue() ?: ""

        if (username == botConfiguration.username) {
            logger().debug("Message comes from myself, ignoring")
            return emptyList()
        }

        val channelType = mapChannelType(item.get("t")?.textValue())

        val channel = RoomMessageHandler.Channel(roomId, roomName, channelType)
        val user = RoomMessageHandler.User(userId, username)
        val message = RoomMessageHandler.Message(messageText, botMessage)

        return roomMessageHandler
            .handle(channel, user, message)
            .map {
                MessageHelper.instance.createSendMessage(roomId, it.message, botConfiguration.botId, it.emoji, it.username)
            }
    }

    private fun mapChannelType(t: String?) = when (t) {
        "c" -> RoomMessageHandler.ChannelType.CHANNEL
        "d" -> RoomMessageHandler.ChannelType.DIRECT
        else -> RoomMessageHandler.ChannelType.OTHER
    }
}
