package at.rueckgr.kotlin.rocketbot

interface RoomMessageHandler {
    fun handle(channel: Channel, user: User, message: Message): List<OutgoingMessage>

    data class Channel(val id: String, val name: String?, val type: ChannelType)

    data class User(val id: String, val username: String)

    data class Message(val message: String, val botMessage: Boolean)

    enum class ChannelType {
        CHANNEL,
        DIRECT,
        OTHER
    }
}
