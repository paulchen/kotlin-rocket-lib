package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.exception.LoginException
import at.rueckgr.kotlin.rocketbot.exception.TerminateWebsocketClientException
import at.rueckgr.kotlin.rocketbot.handler.message.AbstractMessageHandler
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.MessageHelper
import at.rueckgr.kotlin.rocketbot.util.ReconnectWaitService
import at.rueckgr.kotlin.rocketbot.util.logger
import at.rueckgr.kotlin.rocketbot.websocket.ConnectMessage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.reflections.Reflections
import java.util.concurrent.ArrayBlockingQueue


class Bot(private val botConfiguration: BotConfiguration,
          private val roomMessageHandler: RoomMessageHandler,
          private val webserviceUserValidator: WebserviceUserValidator) : Logging {
    companion object {
        val webserviceMessageQueue = ArrayBlockingQueue<WebserviceMessage>(10)
    }

    fun start() {
        logger().info(
            "Configuration: host={}, username={}, ignoredChannels={}, webservicePort={}",
            botConfiguration.host, botConfiguration.username, botConfiguration.ignoredChannels, botConfiguration.webservicePort
        )

        val webservice = Webservice(botConfiguration.webservicePort, webserviceUserValidator)
        try {
            webservice.start()
        }
        catch (e: Exception) {
            logger().error("Error while creating webservice", e)
            return
        }
        runBlocking { runWebsocketClient() }

        logger().debug("Shutting down bot")
        webservice.stop()
    }

    private suspend fun runWebsocketClient() {
        while (true) {
            try {
                val client = HttpClient(CIO) {
                    install(WebSockets)
                }
                client.wss(
                    method = HttpMethod.Get,
                    host = botConfiguration.host,
                    path = "/websocket"
                ) {
                    try {
                        val messageOutputRoutine = async { receiveMessages() }
                        val userInputRoutine = async { sendMessage(ConnectMessage()) }
                        val webserviceMessageRoutine = async { waitForWebserviceInput() }

                        userInputRoutine.await()
                        messageOutputRoutine.await()
                        webserviceMessageRoutine.await()
                    } catch (e: Exception) {
                        logger().error("Websocket error", e)
                    }
                }
            }
            catch (e: Exception) {
                logger().error("Error during (re)connect", e)
            }

            ReconnectWaitService.instance.wait()

            logger().info("Websocket closed, trying to reconnect")
        }
    }

    private suspend fun DefaultClientWebSocketSession.waitForWebserviceInput() {
        withContext(Dispatchers.IO) {
            while (true) {
                val webserviceInput = webserviceMessageQueue.take()
                sendMessage(MessageHelper.instance.createSendMessage(
                    webserviceInput.roomId, webserviceInput.message, botConfiguration.botId, webserviceInput.emoji ?: ""))
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: Any) {
        // TODO implement token refresh

        @Suppress("BlockingMethodInNonBlockingContext")
        val jsonMessage = ObjectMapper().writeValueAsString(message)
        logger().debug("Outgoing message: {}", jsonMessage)
        send(Frame.Text(jsonMessage))
    }

    private suspend fun DefaultClientWebSocketSession.receiveMessages() {
        val handlers = Reflections(AbstractMessageHandler::class.java.packageName)
            .getSubTypesOf(AbstractMessageHandler::class.java)
            .map {
                it
                    .getDeclaredConstructor(RoomMessageHandler::class.java, BotConfiguration::class.java)
                    .newInstance(roomMessageHandler, botConfiguration)
            }
            .associateBy { it.getHandledMessage() }


        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                val text = message.readText()
                logger().debug("Incoming message: {}", text)

                @Suppress("BlockingMethodInNonBlockingContext") val data = ObjectMapper().readTree(text)
                val messageType = data.get("msg")?.textValue() ?: continue
                if(messageType !in handlers) {
                    logger().info("Unknown message type \"{}\", ignoring message", messageType)
                    continue
                }

                try {
                    handlers[messageType]
                        ?.handleMessage(data, getTimestamp(data))
                        ?.forEach { sendMessage(it) }
                }
                catch (e: LoginException) {
                    logger().error(e.message, e)
                    throw TerminateWebsocketClientException()
                }
                catch (e: Exception) {
                    logger().error(e.message, e)
                }
            }

            logger().info("Websocket closed by server")
        }
        catch (e: TerminateWebsocketClientException) {
            throw e
        }
        catch (e: Exception) {
            logger().error("Error while receiving", e)
        }
    }

    private fun getTimestamp(jsonNode: JsonNode): Long {
        val dateNode = jsonNode.get("fields")
            ?.get("args")
            ?.get(1)
            ?.get("lastMessage")
            ?.get("ts")
            ?.get("\$date") ?: return 0L
        if (dateNode.isLong) {
            return dateNode.asLong()
        }
        return 0L
    }
}

