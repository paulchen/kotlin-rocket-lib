package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import com.fasterxml.jackson.databind.JsonNode

class RoomMessageStreamHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractStreamHandler(roomMessageHandler, botConfiguration), Logging {
    override fun getHandledStream() = "stream-room-messages"

    @Suppress("UNCHECKED_CAST")
    override fun handleStreamMessage(data: JsonNode): List<List<Any>> {
        val args = data.get("fields")?.get("args") ?: emptyList()

        return args.map { MessageProcessor.instance.handleStreamMessageItem(roomMessageHandler, botConfiguration, it) }
    }

}
