package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.websocket.PongMessage
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

class PingMessageHandler(eventHandler: EventHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(eventHandler, botConfiguration) {
    companion object {
        var lastPing: LocalDateTime = LocalDateTime.now()

        fun updateLastPing() {
            lastPing = LocalDateTime.now()
        }
    }

    override fun getHandledMessage() = "ping"

    override fun handleMessage(data: JsonNode, timestamp: Long): Array<Any> {
        updateLastPing()
        return arrayOf(PongMessage())
    }
}
