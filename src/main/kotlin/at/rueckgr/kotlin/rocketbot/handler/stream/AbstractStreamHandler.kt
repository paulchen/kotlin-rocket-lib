package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import com.fasterxml.jackson.databind.JsonNode

abstract class AbstractStreamHandler(val eventHandler: EventHandler, val botConfiguration: BotConfiguration) {
    abstract fun getHandledStream(): String

    abstract fun handleStreamMessage(data: JsonNode): List<List<Any>>
}
