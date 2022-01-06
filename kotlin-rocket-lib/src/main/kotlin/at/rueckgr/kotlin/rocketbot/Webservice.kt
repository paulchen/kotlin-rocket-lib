package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.handler.message.PingMessageHandler.Companion.lastPing
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.apache.commons.lang3.StringUtils
import java.time.LocalDateTime


class Webservice(private val webserverPort: Int, private val webserviceUserValidator: WebserviceUserValidator) : Logging {
    private val warningSeconds = 60L
    private val criticalSeconds = 120L

    private var engine: NettyApplicationEngine? = null

    fun start() {
        logger().info("Starting webserver on port {}", webserverPort)

        engine = embeddedServer(Netty, webserverPort) {
            install(ContentNegotiation) {
                jackson {
                    findAndRegisterModules()
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            install(Authentication) {
                basic(name = "basic-auth") {
                    realm = "kotlin-rocket-bot"
                    validate { credentials -> verifyCredentials(credentials) }
                }
            }
            routing {
                route("/status") {
                    get {
                        call.respond(getStatus())
                    }
                }
                authenticate("basic-auth") {
                    route("/message") {
                        post {
                            val message = call.receive<WebserviceMessage>()

                            logger().debug("Received message via webservice: {}", message)
                            val (validationMessage, status, validatedMessage) = validateMessage(message)
                            if (StringUtils.isNotBlank(validationMessage)) {
                                call.respondText(validationMessage, status = status)
                            }
                            else {
                                Bot.webserviceMessageQueue.add(validatedMessage)
                                call.respondText("Message submitted successfully", status = HttpStatusCode.Created)
                            }
                        }
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        logger().info("Stopping webserver")

        engine?.stop(100L, 100L)
    }

    private fun validateMessage(message: WebserviceMessage): ValidationResult {
        if (StringUtils.isBlank(message.roomId) && StringUtils.isBlank(message.roomName)) {
            return ValidationResult("One of roomId and roomName must be set", HttpStatusCode.BadRequest, message)
        }
        if (StringUtils.isNotBlank(message.roomId) && StringUtils.isNotBlank(message.roomName)) {
            return ValidationResult("Only of roomId and roomName must be set", HttpStatusCode.BadRequest, message)
        }
        val roomId = if (StringUtils.isNotBlank(message.roomName)) {
            if (!Bot.knownChannelNamesToIds.containsKey(message.roomName)) {
                return ValidationResult("Unknown channel ${message.roomName}", HttpStatusCode.BadRequest, message)
            }
            Bot.knownChannelNamesToIds[message.roomName]
        }
        else {
            message.roomId
        }

        val validatedMessage = WebserviceMessage(roomId, null, message.message, message.emoji, message.username)
        return ValidationResult("", HttpStatusCode.OK, validatedMessage)
    }

    private fun verifyCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
        when (webserviceUserValidator.validate(credentials.name, credentials.password)) {
            true -> UserIdPrincipal(credentials.name)
            false -> null
        }

    private fun getStatus(): Map<String, Any> {
        val status = if (LocalDateTime.now().minusSeconds(criticalSeconds).isAfter(lastPing)) {
            "CRITICAL"
        } else if (LocalDateTime.now().minusSeconds(warningSeconds).isAfter(lastPing)) {
            "WARNING"
        } else {
            "OK"
        }

        return mapOf(
            "status" to status,
            "lastPing" to lastPing
        )
    }
}

data class WebserviceMessage(
    val roomId: String?,
    val roomName: String?,
    val message: String,
    val emoji: String?,
    val username: String?
)

data class ValidationResult(
    val validationMessage: String,
    val httpStatusCode: HttpStatusCode,
    val webserviceMessage: WebserviceMessage
)
