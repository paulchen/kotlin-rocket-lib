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
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit


class Bot(private val botConfiguration: BotConfiguration,
          private val roomMessageHandler: RoomMessageHandler,
          private val webserviceUserValidator: WebserviceUserValidator,
          private val healthChecker: HealthChecker) : Logging {
    companion object {
        val webserviceMessageQueue = ArrayBlockingQueue<WebserviceMessage>(10)
        val knownChannelNamesToIds = HashMap<String, String>()
        val statusService = StatusService()
    }

    fun start() {
        statusService.healthChecker = this.healthChecker

        logger().info(
            "Configuration: host={}, username={}, ignoredChannels={}, webservicePort={}",
            botConfiguration.host, botConfiguration.username, botConfiguration.ignoredChannels, botConfiguration.webservicePort
        )

        val webservice = Webservice(botConfiguration.webservicePort, webserviceUserValidator, statusService)
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
                        webserviceMessageRoutine.cancelAndJoin()
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
            while (isActive) {
                val webserviceInput = webserviceMessageQueue.poll(5, TimeUnit.SECONDS) ?: continue
                sendMessage(MessageHelper.instance.createSendMessage(
                    webserviceInput.roomId!!,
                    webserviceInput.message,
                    botConfiguration.botId,
                    webserviceInput.emoji,
                    webserviceInput.username
                ))

                Thread.sleep(1000L)
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

