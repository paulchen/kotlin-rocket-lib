package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import com.fasterxml.jackson.databind.JsonNode

abstract class AbstractStreamHandler(val roomMessageHandler: RoomMessageHandler, val botConfiguration: BotConfiguration) {
    abstract fun getHandledStream(): String

    abstract fun handleStreamMessage(data: JsonNode): List<List<Any>>
}
