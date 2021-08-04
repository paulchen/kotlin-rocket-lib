package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode

class NotifyUserStreamHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractStreamHandler(roomMessageHandler, botConfiguration) {
    override fun getHandledStream() = "stream-notify-user"

    @Suppress("UNCHECKED_CAST")
    override fun handleStreamMessage(configuration: BotConfiguration, data: JsonNode): List<List<Any>> {
        val args: JsonNode = data.get("fields")?.get("args") ?: return emptyList()

        if (args.get(0).textValue() != "inserted") {
            return emptyList()
        }

        val items = ArrayList<JsonNode>()
        for (i in 1 until args.size()) {
            items.add(args.get(i))
        }

        return items.map {
            val roomId = it.get("_id")

            if (configuration.ignoredChannels.contains(it.get("fname")?.textValue())) {
                emptyList()
            }
            else {
                listOf(
                    SubscribeMessage(
                        id = "subscribe-$roomId",
                        name = "stream-room-messages",
                        params = arrayOf(roomId, false)
                    )
                )
            }
        }
    }
}
