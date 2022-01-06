package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.handler.stream.AbstractStreamHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.MessageHelper
import at.rueckgr.kotlin.rocketbot.util.logger
import com.fasterxml.jackson.databind.JsonNode
import org.reflections.Reflections

@Suppress("unused")
class ChangedMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration), Logging {
    private val handlers: Map<String, AbstractStreamHandler> =
        Reflections(AbstractStreamHandler::class.java.packageName)
            .getSubTypesOf(AbstractStreamHandler::class.java)
            .map {
                it
                    .getDeclaredConstructor(RoomMessageHandler::class.java, BotConfiguration::class.java)
                    .newInstance(roomMessageHandler, botConfiguration)
            }
            .associateBy { it.getHandledStream() }
    private var newestTimestampSeen = 0L

    override fun getHandledMessage() = "changed"

    override fun handleMessage(data: JsonNode, timestamp: Long): Array<Any> {
        updateRoomNameMapping(data)

        if (timestamp > 0L) {
            if (timestamp <= newestTimestampSeen) {
                logger().debug("Timestamp of message ({}) is not newer than newest timestamp seen ({}), ignoring", timestamp, newestTimestampSeen)
                return emptyArray()
            }
            logger().debug("Updating newest timestamp seen from {} to {}", newestTimestampSeen, timestamp)
            newestTimestampSeen = timestamp
        }

        val collection = data.get("collection")?.textValue() ?: return emptyArray()

        return handlers[collection]!!
            .handleStreamMessage(data)
            .flatten()
            .toTypedArray()
    }

    private fun updateRoomNameMapping(data: JsonNode) {
        val eventName = MessageHelper.instance.getEventName(data) ?: return
        if (eventName == "rooms-changed") {
            val roomDetails = data?.get("fields")?.get("args")?.get(1) ?: return
            val roomId = roomDetails.get("_id")?.textValue() ?: return
            val roomName = roomDetails.get("name")?.textValue() ?: return

            Bot.knownChannelNamesToIds[roomName] = roomId
        }
    }
}
