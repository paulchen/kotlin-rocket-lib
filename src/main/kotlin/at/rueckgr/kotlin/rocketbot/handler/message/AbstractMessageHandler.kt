package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import com.fasterxml.jackson.databind.JsonNode

abstract class AbstractMessageHandler(val roomMessageHandler: RoomMessageHandler, val botConfiguration: BotConfiguration) {
    abstract fun getHandledMessage(): String

    abstract fun handleMessage(data: JsonNode, timestamp: Long): Array<Any>
}
