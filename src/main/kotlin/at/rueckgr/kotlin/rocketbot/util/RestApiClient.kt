package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.BotConfiguration
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking

class RestApiClient(private val botHost: String) : Logging {
    fun updateStatus() {
        val response: UsersSetStatusResponse = runBlocking {
            createHttpClient().post {
                url(buildUrl("users.setStatus"))
                headers(buildHeaders())
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

    fun getInstanceVersion(): String? {
        val statisticsResponse: StatisticsResponse = runBlocking {
            createHttpClient().get {
                url(buildUrl("statistics"))
                headers(buildHeaders())
                contentType(ContentType.Application.Json)
            }.body()
        }

        if (!statisticsResponse.success) {
            logger().error("Failed to get statistics: {}", statisticsResponse.error)
            return null
        }

        return statisticsResponse.version
    }

    private fun buildHeaders(): HeadersBuilder.() -> Unit = {
        append("X-Auth-Token", Bot.authToken!!)
        append("X-User-Id", Bot.userId!!)
    }

    private fun createHttpClient() = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }

    private fun buildUrl(endpoint: String) = "https://$botHost/api/v1/$endpoint"

    data class UsersSetStatusResponse(val success: Boolean)

    data class StatisticsResponse(val version: String?, val success: Boolean, val error: String?)
}
