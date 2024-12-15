package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.EventHandler
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

    fun getUsersByRole(role: String): List<EventHandler.User> {
        val limit = 50
        var offset = 0

        val users = mutableListOf<EventHandler.User>()
        do {
            val retrievedUsers = getUsersByRole(role, offset, limit)
            users.addAll(retrievedUsers)
            offset += limit
        } while (retrievedUsers.size == limit)

        return users
    }

    private fun getUsersByRole(role: String, offset: Int, @Suppress("SameParameterValue") limit: Int): List<EventHandler.User> {
        logger().debug("Invoking roles.getUsersInRole with role {}, limit {}, offset {}", role, offset, limit)
        val response: UsersListResponse = runBlocking {
            try {
                createHttpClient().get {
                    url {
                        url(buildUrl("roles.getUsersInRole"))
                        parameter("role", role)
                        parameter("limit", limit)
                        parameter("offset", offset)
                    }
                    headers(buildHeaders())
                    contentType(ContentType.Application.Json)
                }.body()
            }
            catch (e: Exception) {
                logger().error(e.message)
                UsersListResponse(emptyList(), false, e.message)
            }
        }

        if (!response.success || response.users == null) {
            logger().error("Failed to get list of users with role {} (limit {}, offset {}), error: {}", role, limit, offset, response.error)
            return emptyList()
        }

        return response.users.map { EventHandler.User(it._id, it.username) }
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

    data class UsersListResponse(val users: List<RestUser>?, val success: Boolean, val error: String?)

    data class RestUser(val _id: String, val username: String, val name: String)
}
