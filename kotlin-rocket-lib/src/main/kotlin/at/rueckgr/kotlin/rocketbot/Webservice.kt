package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.handler.message.PingMessageHandler.Companion.lastPing
import at.rueckgr.kotlin.rocketbot.util.Logging
import at.rueckgr.kotlin.rocketbot.util.logger
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.time.LocalDateTime

class Webservice : Logging {
    private val warningSeconds = 60L
    private val criticalSeconds = 120L
    private val webserverPort = 8080

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
            routing {
                route("/status") {
                    get {
                        call.respond(getStatus())
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        logger().info("Stopping webserver")

        engine?.stop(100L, 100L)
    }

    private fun getStatus(): Map<String, Any> {
        val status = if (LocalDateTime.now().minusSeconds(criticalSeconds).isAfter(lastPing)) {
            "CRITICAL"
        }
        else if (LocalDateTime.now().minusSeconds(warningSeconds).isAfter(lastPing)) {
            "WARNING"
        }
        else {
            "OK"
        }

        return mapOf(
            "status" to status,
            "lastPing" to lastPing
        )
    }
}
