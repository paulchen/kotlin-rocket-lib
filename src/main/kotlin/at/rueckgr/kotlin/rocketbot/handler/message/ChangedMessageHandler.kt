package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.handler.stream.AbstractStreamHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import com.fasterxml.jackson.databind.JsonNode
import org.reflections.Reflections

@Suppress("unused")
class ChangedMessageHandler(eventHandler: EventHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(eventHandler, botConfiguration), Logging {
    private val handlers: Map<String, AbstractStreamHandler> =
        Reflections(AbstractStreamHandler::class.java.packageName)
            .getSubTypesOf(AbstractStreamHandler::class.java)
            .map {
                it
                    .getDeclaredConstructor(EventHandler::class.java, BotConfiguration::class.java)
                    .newInstance(eventHandler, botConfiguration)
            }
            .associateBy { it.getHandledStream() }

    override fun getHandledMessage() = "changed"

    override fun handleMessage(data: JsonNode): Array<Any> {
        val collection = data.get("collection")?.textValue() ?: return emptyArray()

        return handlers[collection]!!
            .handleStreamMessage(data)
            .flatten()
            .toTypedArray()
    }
}
