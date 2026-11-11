package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.MessageQueueItem
import org.apache.commons.lang3.StringUtils

class MessageHelper {
    companion object {
        val instance = MessageHelper()
    }

    fun mapChannelType(t: String?) = when (t) {
        "c" -> EventHandler.ChannelType.CHANNEL
        "d" -> EventHandler.ChannelType.DIRECT
        else -> EventHandler.ChannelType.OTHER
    }

    fun validateMessage(message: MessageQueueItem): ValidationResult {
        if (StringUtils.isBlank(message.roomId) && StringUtils.isBlank(message.roomName)) {
            return ValidationResult("One of roomId and roomName must be set", message)
        }
        if (StringUtils.isNotBlank(message.roomId) && StringUtils.isNotBlank(message.roomName)) {
            return ValidationResult("Only one of roomId and roomName must be set", message)
        }
        val roomId = if (message.roomName != null && StringUtils.isNotBlank(message.roomName)) {
            Bot.subscriptionService.getChannelIdByName(message.roomName)
                ?: return ValidationResult("Unknown channel ${message.roomName}", message)
        }
        else {
            message.roomId
        }

        val validatedMessage = MessageQueueItem(roomId, null, message.message, message.parentMessageId, message.emoji, message.username)
        return ValidationResult("", validatedMessage)
    }
}

data class ValidationResult(
    val validationMessage: String,
    val validatedMessage: MessageQueueItem
)

