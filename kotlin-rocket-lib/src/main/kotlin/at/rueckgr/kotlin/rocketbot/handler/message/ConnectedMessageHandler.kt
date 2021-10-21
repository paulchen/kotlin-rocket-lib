package at.rueckgr.kotlin.rocketbot.handler.message

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.websocket.LoginMessage
import at.rueckgr.kotlin.rocketbot.websocket.PasswordData
import at.rueckgr.kotlin.rocketbot.websocket.UserData
import at.rueckgr.kotlin.rocketbot.websocket.WebserviceRequestParam
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.codec.digest.DigestUtils

class ConnectedMessageHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractMessageHandler(roomMessageHandler, botConfiguration) {
    override fun getHandledMessage() = "connected"

    override fun handleMessage(data: JsonNode, timestamp: Long): Array<Any> {
        val digest = DigestUtils.sha256Hex(botConfiguration.password)
        return arrayOf(
            LoginMessage(
                id = "login-initial",
                params = arrayOf(
                    WebserviceRequestParam(
                        UserData(botConfiguration.username),
                        PasswordData(digest, "sha-256")
                    )
                )
            )
        )
    }
}
