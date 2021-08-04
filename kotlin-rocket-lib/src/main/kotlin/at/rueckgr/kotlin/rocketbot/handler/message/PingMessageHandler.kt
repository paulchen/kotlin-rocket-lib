package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.websocket.PongMessage
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

class PingMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration) {
    companion object {
        var lastPing: LocalDateTime = LocalDateTime.now()
    }

    override fun getHandledMessage() = "ping"

    override fun handleMessage(configuration: BotConfiguration, data: JsonNode, timestamp: Long): Array<Any> {
        lastPing = LocalDateTime.now()
        return arrayOf(PongMessage())
    }
}
