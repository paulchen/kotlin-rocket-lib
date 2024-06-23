package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.WebserviceMessage
import at.rueckgr.kotlin.rocketbot.websocket.SendMessageMessage
import org.apache.commons.lang3.StringUtils
import java.util.*

class MessageHelper {
    companion object {
        val instance = MessageHelper()
    }

    fun createSendMessage(roomId: String, message: String, botId: String, parentMessageId: String? = null, emoji: String? = null, username: String? = null): SendMessageMessage {
        val id = UUID.randomUUID().toString()
        val botTag = mapOf("i" to botId)
        val params = mutableMapOf("_id" to id, "rid" to roomId, "msg" to message, "bot" to botTag)
        if (StringUtils.isNotBlank(parentMessageId)) {
            params["tmid"] = parentMessageId!!
        }
        if (StringUtils.isNotBlank(emoji)) {
            params["emoji"] = emoji!!
        }
        if (StringUtils.isNotBlank(username)) {
            params["alias"] = username!!
        }
        return SendMessageMessage(id = id, params = listOf(params))
    }

    fun mapChannelType(t: String?) = when (t) {
        "c" -> EventHandler.ChannelType.CHANNEL
        "d" -> EventHandler.ChannelType.DIRECT
        else -> EventHandler.ChannelType.OTHER
    }

    fun validateMessage(message: WebserviceMessage): ValidationResult {
        if (StringUtils.isBlank(message.roomId) && StringUtils.isBlank(message.roomName)) {
            return ValidationResult("One of roomId and roomName must be set", message)
        }
        if (StringUtils.isNotBlank(message.roomId) && StringUtils.isNotBlank(message.roomName)) {
            return ValidationResult("Only of roomId and roomName must be set", message)
        }
        val roomId = if (message.roomName != null && StringUtils.isNotBlank(message.roomName)) {
            Bot.subscriptionService.getChannelIdByName(message.roomName)
                ?: return ValidationResult("Unknown channel ${message.roomName}", message)
        }
        else {
            message.roomId
        }

        val validatedMessage = WebserviceMessage(roomId, null, message.message, message.parentMessageId, message.emoji, message.username)
        return ValidationResult("", validatedMessage)
    }
}

data class ValidationResult(
    val validationMessage: String,
    val webserviceMessage: WebserviceMessage
)

