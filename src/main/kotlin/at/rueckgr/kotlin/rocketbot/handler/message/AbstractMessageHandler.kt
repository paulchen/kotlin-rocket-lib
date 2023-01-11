package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import com.fasterxml.jackson.databind.JsonNode

abstract class AbstractMessageHandler(val eventHandler: EventHandler, val botConfiguration: BotConfiguration) {
    abstract fun getHandledMessage(): String

    abstract fun handleMessage(data: JsonNode): Array<Any>
}
