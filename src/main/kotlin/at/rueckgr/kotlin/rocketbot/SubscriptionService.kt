package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import at.rueckgr.kotlin.rocketbot.websocket.UnsubscribeMessage
import java.util.*

class SubscriptionService : Logging {
    private val channelsById = HashMap<String, ChannelData>()
    private val channelsByName = HashMap<String, ChannelData>()
    private val newestTimestampsSeen = HashMap<String, Long>()

    fun handleSubscription(channelId: String, channelName: String?, channelType: EventHandler.ChannelType, timestamp: Long): SubscribeMessage? {
        logger().info("Subscribing to channel with id {} and name {}", channelId, channelName)

        if (channelsById.contains(channelId)) {
            logger().info("Already subscribed to channel {}", channelId)
            return null
        }
        val subscriptionId = UUID.randomUUID().toString()
        val channelData = ChannelData(channelId, channelName, channelType, subscriptionId)
        if (channelName != null) {
            channelsByName[channelName] = channelData
        }
        channelsById[channelId] = channelData
        newestTimestampsSeen[channelId] = timestamp

        return SubscribeMessage(id = subscriptionId, name = "stream-room-messages", params = arrayOf(channelId, false))
    }

    fun handleUnsubscription(channelId: String): UnsubscribeMessage? {
        logger().info("Unsubscribing from {}", channelId)

        val channel = channelsById[channelId]
        if (channel == null) {
            logger().error("Unknown channel {}", channelId)
            return null
        }

        channelsById.remove(channel.id)
        if (channel.name != null) {
            channelsByName.remove(channel.name)
        }

        return UnsubscribeMessage(id = channel.subscriptionId)
    }

    fun getChannelIdByName(roomName: String) = channelsByName[roomName]?.id

    fun getChannelNameById(roomId: String) = channelsById[roomId]?.name

    fun getNewestTimestampSeen(roomId: String): Long? = newestTimestampsSeen[roomId]

    fun updateNewestTimestampSeen(roomId: String, timestamp: Long) {
        newestTimestampsSeen[roomId] = timestamp
    }

    fun getRoomType(roomId: String) = channelsById[roomId]?.type

    fun reset() {
        channelsById.clear()
        channelsByName.clear()
        newestTimestampsSeen.clear()
    }
}

data class ChannelData(val id: String, val name: String?, val type: EventHandler.ChannelType, val subscriptionId: String)
