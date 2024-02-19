package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.apache.commons.lang3.StringUtils


@Suppress("ExtractKtorModule")
class Webservice(private val webserverPort: Int,
                 private val webserviceUserValidator: WebserviceUserValidator,
                 private val statusService: StatusService) : Logging {
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
                        call.respond(statusService.getStatus())
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
        val roomId = if (message.roomName != null && StringUtils.isNotBlank(message.roomName)) {
            Bot.subscriptionService.getChannelIdByName(message.roomName)
                ?: return ValidationResult("Unknown channel ${message.roomName}", HttpStatusCode.BadRequest, message)
        }
        else {
            message.roomId
        }

        val validatedMessage = WebserviceMessage(roomId, null, message.message, message.parentMessageId, message.emoji, message.username)
        return ValidationResult("", HttpStatusCode.OK, validatedMessage)
    }

    private fun verifyCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
        when (webserviceUserValidator.validate(credentials.name, credentials.password)) {
            true -> UserIdPrincipal(credentials.name)
            false -> null
        }
}

data class WebserviceMessage(
    val roomId: String?,
    val roomName: String?,
    val message: String,
    val parentMessageId: String? = null,
    val emoji: String? = null,
    val username: String? = null
)

data class ValidationResult(
    val validationMessage: String,
    val httpStatusCode: HttpStatusCode,
    val webserviceMessage: WebserviceMessage
)
