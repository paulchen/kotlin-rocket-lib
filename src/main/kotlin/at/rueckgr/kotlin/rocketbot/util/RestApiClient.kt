package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking

class RestApiClient(private val botConfiguration: BotConfiguration) : Logging {
    fun updateStatus() {
        val response: SimpleResponse = runBlocking {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    jackson()
                }
            }.post {
                url("https://${botConfiguration.host}/api/v1/users.setStatus")
                headers {
                    append("X-Auth-Token", Bot.authToken!!)
                    append("X-User-Id", Bot.userId!!)
                }
                contentType(ContentType.Application.Json)
                setBody(object {
                    @Suppress("unused") val message = ""
                    @Suppress("unused") val status = "online"
                })
            }.body()
        }

        if (!response.success) {
            logger().error("Failed to set user status")
        }
    }

    data class SimpleResponse(val success: Boolean)
}
