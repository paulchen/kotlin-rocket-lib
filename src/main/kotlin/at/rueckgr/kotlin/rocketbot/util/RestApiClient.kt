package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.EventHandler
import at.rueckgr.kotlin.rocketbot.MessageQueueItem
import at.rueckgr.kotlin.rocketbot.generated.messaging.api.ChatApi
import at.rueckgr.kotlin.rocketbot.generated.messaging.model.PostApiV1ChatSendMessageRequest
import at.rueckgr.kotlin.rocketbot.generated.messaging.model.PostApiV1ChatSendMessageRequestMessage
import at.rueckgr.kotlin.rocketbot.generated.statistics.api.StatisticsApi
import at.rueckgr.kotlin.rocketbot.generated.usermanagement.api.RolesApi
import at.rueckgr.kotlin.rocketbot.generated.usermanagement.api.UsersApi
import at.rueckgr.kotlin.rocketbot.generated.usermanagement.model.PostApiV1UsersSetStatusRequest
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

class RestApiClient internal constructor(
    private val botHost: String,
    private val httpEngineFactory: () -> HttpClientEngine,
) : Logging {
    constructor(botHost: String) : this(botHost, { CIO.create() })

    fun sendMessage(message: MessageQueueItem): Boolean {
        return try {
            runBlocking {
                httpEngineFactory().use { engine ->
                    val response = createChatApi(engine).postApiV1ChatSendMessage(
                        xAuthToken = Bot.authToken!!,
                        xUserId = Bot.userId!!,
                        postApiV1ChatSendMessageRequest = PostApiV1ChatSendMessageRequest(
                            message = PostApiV1ChatSendMessageRequestMessage(
                                rid = requireNotNull(message.roomId),
                                msg = message.message,
                                tmid = message.parentMessageId,
                                emoji = message.emoji,
                                alias = message.username,
                            ),
                        ),
                    )
                    response.success && response.body().success == true
                }
            }
        }
        catch (e: Exception) {
            logger().error("Failed to send message to room {}", message.roomId, e)
            false
        }
    }

    fun updateStatus() {
        val success = runBlocking {
            httpEngineFactory().use { engine ->
                val response = createUsersApi(engine).postApiV1UsersSetStatus(
                    xUserId = Bot.userId!!,
                    xAuthToken = Bot.authToken!!,
                    postApiV1UsersSetStatusRequest = PostApiV1UsersSetStatusRequest(
                        message = "",
                        status = "online",
                    ),
                )
                response.success && response.body().success == true
            }
        }

        if (!success) {
            logger().error("Failed to set user status")
        }
    }

    fun getInstanceVersion(): String? = runBlocking {
        httpEngineFactory().use { engine ->
            val response = createStatisticsApi(engine).getApiV1Statistics(
                xAuthToken = Bot.authToken!!,
                xUserId = Bot.userId!!,
            )
            if (!response.success) {
                logger().error("Failed to get statistics (HTTP {}): {}", response.status, response.response.bodyAsText())
                return@runBlocking null
            }
            val statistics = response.body()
            if (statistics.success != true) {
                logger().error("Failed to get statistics: {}", response.response.bodyAsText())
                return@runBlocking null
            }
            statistics.version
        }
    }

    fun getUsersByRole(role: String): List<EventHandler.User> = runBlocking {
        httpEngineFactory().use { engine ->
            val api = createRolesApi(engine)
            val limit = 50
            var offset = 0
            val users = mutableListOf<EventHandler.User>()
            do {
                val retrievedUsers = getUsersByRole(api, role, offset, limit)
                users.addAll(retrievedUsers)
                offset += limit
            } while (retrievedUsers.size == limit)
            users
        }
    }

    private suspend fun getUsersByRole(api: RolesApi, role: String, offset: Int, limit: Int): List<EventHandler.User> {
        return try {
            val response = api.getApiV1RolesGetUsersInRole(
                role = role,
                xUserId = Bot.userId!!,
                xAuthToken = Bot.authToken!!,
                offset = offset.toLong(),
                count = limit.toLong(),
            )
            if (!response.success) {
                logger().error(
                    "Failed to get list of users with role {} (limit {}, offset {}), HTTP {}: {}",
                    role, limit, offset, response.status, response.response.bodyAsText(),
                )
                return emptyList()
            }
            val result = response.body()
            val users = result.users
            if (result.success != true || users == null) {
                logger().error(
                    "Failed to get list of users with role {} (limit {}, offset {}): {}",
                    role, limit, offset, response.response.bodyAsText(),
                )
                return emptyList()
            }
            users.map { EventHandler.User(requireNotNull(it.id), requireNotNull(it.username)) }
        }
        catch (e: Exception) {
            logger().error("Failed to get list of users with role {} (limit {}, offset {})", role, limit, offset, e)
            emptyList()
        }
    }

    private fun createChatApi(engine: HttpClientEngine) = ChatApi(
        baseUrl = "https://$botHost",
        httpClientEngine = engine,
        httpClientConfig = { it.configureRequestLogging() },
        jsonBlock = { configureJson() },
    )

    private fun createUsersApi(engine: HttpClientEngine) = UsersApi(
        baseUrl = "https://$botHost",
        httpClientEngine = engine,
        httpClientConfig = { it.configureRequestLogging() },
        jsonBlock = { configureJson() },
    )

    private fun createRolesApi(engine: HttpClientEngine) = RolesApi(
        baseUrl = "https://$botHost",
        httpClientEngine = engine,
        httpClientConfig = { it.configureRequestLogging() },
        jsonBlock = { configureJson() },
    )

    private fun createStatisticsApi(engine: HttpClientEngine) = StatisticsApi(
        baseUrl = "https://$botHost",
        httpClientEngine = engine,
        httpClientConfig = { it.configureRequestLogging() },
        jsonBlock = { configureJson() },
    )

    private fun ObjectMapper.configureJson() {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    }

    private fun HttpClientConfig<*>.configureRequestLogging() {
        install(RestApiLogging)
    }
}
