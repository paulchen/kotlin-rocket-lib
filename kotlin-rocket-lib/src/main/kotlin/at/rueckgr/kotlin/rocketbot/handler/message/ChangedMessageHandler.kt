package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.handler.stream.AbstractStreamHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import com.fasterxml.jackson.databind.JsonNode
import org.reflections.Reflections

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

    override fun handleMessage(configuration: BotConfiguration, data: JsonNode, timestamp: Long): Array<Any> {
        if (timestamp < newestTimestampSeen) {
            logger().debug("Timestamp of message ({}) is older than newest timestamp seen ({}), ignoring", timestamp, newestTimestampSeen)
            return emptyArray()
        }
        logger().debug("Updating newest timestamp seen from {} to {}", newestTimestampSeen, timestamp)
        newestTimestampSeen = timestamp

        val collection = data.get("collection")?.textValue() ?: return emptyArray()

        return handlers[collection]!!
            .handleStreamMessage(configuration, data)
            .flatten()
            .toTypedArray()
    }
}
