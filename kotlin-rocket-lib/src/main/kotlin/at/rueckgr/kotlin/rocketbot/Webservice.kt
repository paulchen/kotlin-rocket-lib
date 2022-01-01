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
                            Bot.webserviceMessageQueue.add(message)
                            call.respondText("Message submitted successfully", status = HttpStatusCode.Created)
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
    val roomId: String,
    val message: String
)
