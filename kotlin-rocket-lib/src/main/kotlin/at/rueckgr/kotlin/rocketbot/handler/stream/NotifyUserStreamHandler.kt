package at.rueckgr.kotlin.rocketbot.handler.stream

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import at.rueckgr.kotlin.rocketbot.RoomMessageHandler
import at.rueckgr.kotlin.rocketbot.websocket.SubscribeMessage
import com.fasterxml.jackson.databind.JsonNode
import java.util.*
import kotlin.collections.ArrayList

class NotifyUserStreamHandler(roomMessageHandler: RoomMessageHandler, botConfiguration: BotConfiguration)
        : AbstractStreamHandler(roomMessageHandler, botConfiguration) {
    override fun getHandledStream() = "stream-notify-user"

    @Suppress("UNCHECKED_CAST")
    override fun handleStreamMessage(data: JsonNode): List<List<Any>> {
        val args: JsonNode = data.get("fields")?.get("args") ?: return emptyList()

        return when (args.get(0).textValue()) {
            "inserted" -> handleChannelMessage(args)
            "updated" -> handleDirectMessage(args)
            else -> emptyList()
        }
    }

    private fun handleChannelMessage(args: JsonNode): List<List<Any>> {
        val items = ArrayList<JsonNode>()
        for (i in 1 until args.size()) {
            items.add(args.get(i))
        }

        return items.map {
            val roomId = it.get("_id")

            if (botConfiguration.ignoredChannels.contains(it.get("fname")?.textValue())) {
                emptyList()
            }
            else {
                listOf(
                    SubscribeMessage(
                        id = UUID.randomUUID().toString(),
                        name = "stream-room-messages",
                        params = arrayOf(roomId, false)
                    )
                )
            }
        }
    }

    private fun handleDirectMessage(args: JsonNode): List<List<Any>> {
        val items = ArrayList<JsonNode>()
        for (i in 1 until args.size()) {
            items.add(args.get(i))
        }

        return items
            .filter { it.get("t").textValue() == "d" }
            .map { MessageProcessor.instance.handleStreamMessageItem(roomMessageHandler, botConfiguration, it.get("lastMessage")) }
    }
}
