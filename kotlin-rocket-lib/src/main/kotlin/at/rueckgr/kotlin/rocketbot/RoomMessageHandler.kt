package at.rueckgr.kotlin.rocketbot

interface RoomMessageHandler {
    fun handle(username: String, message: String, botMessage: Boolean): List<OutgoingMessage>
}
