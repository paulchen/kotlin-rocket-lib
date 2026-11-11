package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.Bot
import at.rueckgr.kotlin.rocketbot.EventHandler
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import java.util.concurrent.CopyOnWriteArrayList

class RestApiClientTest {
    @Test
    fun `status update sends credentials and typed JSON to the configured host`() {
        var requests = 0
        val errors = withResponse("""{"success":true,"futureField":"ignored"}""") { request ->
            requests++
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://chat.example.org/api/v1/users.setStatus", request.url.toString())
            assertEquals("test-user", request.headers["X-User-Id"])
            assertEquals("test-token", request.headers["X-Auth-Token"])
            val body = jacksonObjectMapper().readTree(request.body.toByteArray())
            assertEquals(jacksonObjectMapper().readTree("""{"message":"","status":"online"}"""), body)
        }
        assertEquals(1, requests)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `unsuccessful status response is logged`() {
        val errors = withResponse("""{"success":false}""")
        assertEquals(listOf("Failed to set user status"), errors)
    }

    @Test
    fun `HTTP error is logged without trying to decode a success response`() {
        val errors = withResponse("Unauthorized", HttpStatusCode.Unauthorized)
        assertEquals(listOf("Failed to set user status"), errors)
    }

    @Test
    fun `DEBUG logs contain request and response but hide authentication headers`() {
        withResponse(
            """{"success":true}""",
            verifyLogs = { events ->
                val lines = events.filter { it.level == Level.DEBUG }.map { it.message.formattedMessage }
                assertEquals(listOf(
                    "Request POST https://chat.example.org/api/v1/users.setStatus body={\"message\":\"\",\"status\":\"online\"}",
                    "Response POST https://chat.example.org/api/v1/users.setStatus code=200 body={\"success\":true}",
                ), lines)
                val debug = lines.joinToString("\n")
                assertFalse(debug.contains("X-Auth-Token", ignoreCase = true))
                assertFalse(debug.contains("test-token"))
                assertFalse(debug.contains("test-user"))
            },
        )
    }

    @Test
    fun `multiline response body stays on one log line`() {
        withResponse(
            "{\n  \"success\": true\n}",
            verifyLogs = { events ->
                val lines = events.filter { it.level == Level.DEBUG }.map { it.message.formattedMessage }
                assertEquals(2, lines.size)
                assertTrue(lines.last().endsWith("body={\"success\":true}"))
                assertTrue(lines.all { '\n' !in it && '\r' !in it })
            },
        )
    }

    @Test
    fun `multiline text error response stays on one log line`() {
        withResponse(
            "Not authorized\nPlease log in\r\n",
            status = HttpStatusCode.Unauthorized,
            verifyLogs = { events ->
                val lines = events.filter { it.level == Level.DEBUG }.map { it.message.formattedMessage }
                assertEquals(2, lines.size)
                assertTrue(lines.last().endsWith("code=401 body=Not authorized\\nPlease log in\\r\\n"))
                assertTrue(lines.all { '\n' !in it && '\r' !in it })
            },
        )
    }

    @Test
    fun `request and response logging is disabled at INFO`() {
        withResponse(
            """{"success":true}""",
            logLevel = Level.INFO,
            verifyLogs = { events -> assertTrue(events.isEmpty()) },
        )
    }

    @Test
    fun `statistics uses the configured host and credentials and returns the version`() {
        var version: String? = null
        val errors = withResponse(
            """{"success":true,"version":"8.0.0","futureField":"ignored"}""",
            invokeClient = { version = it.getInstanceVersion() },
        ) { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https://chat.example.org/api/v1/statistics", request.url.toString())
            assertEquals("test-user", request.headers["X-User-Id"])
            assertEquals("test-token", request.headers["X-Auth-Token"])
        }
        assertEquals("8.0.0", version)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `statistics returns null and logs server failures`() {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.Forbidden)) {
            val errors = withResponse(
                """{"success":false,"error":"not-authorized"}""",
                status = status,
                invokeClient = { assertNull(it.getInstanceVersion()) },
            )
            assertTrue(errors.single().contains("not-authorized"))
        }
    }

    @Test
    fun `roles query paginates with count and offset and maps user IDs`() {
        val offsets = mutableListOf<Int>()
        val mapper = jacksonObjectMapper()
        var users = emptyList<EventHandler.User>()
        val errors = withResponse(
            response = "",
            verifyLogs = { events ->
                val lines = events.filter { it.level == Level.DEBUG }.map { it.message.formattedMessage }
                assertEquals(4, lines.size)
                assertTrue(lines[0].startsWith("Request GET "))
                assertTrue(lines[0].endsWith("body=<empty>"))
                assertTrue(lines[1].startsWith("Response GET "))
                assertTrue(lines[2].startsWith("Request GET "))
                assertTrue(lines[3].startsWith("Response GET "))
            },
            responseForRequest = { request ->
                val offset = request.url.parameters["offset"]!!.toInt()
                val count = if (offset == 0) 50 else 1
                mapper.writeValueAsString(mapOf(
                    "success" to true,
                    "users" to (offset until offset + count).map {
                        mapOf("_id" to "user-$it", "username" to "name-$it", "futureField" to true)
                    },
                ))
            },
            invokeClient = { users = it.getUsersByRole("bot") },
        ) { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/roles.getUsersInRole", request.url.encodedPath)
            assertEquals("bot", request.url.parameters["role"])
            assertEquals("50", request.url.parameters["count"])
            assertNull(request.url.parameters["limit"])
            assertEquals("test-user", request.headers["X-User-Id"])
            assertEquals("test-token", request.headers["X-Auth-Token"])
            offsets += request.url.parameters["offset"]!!.toInt()
        }
        assertEquals(listOf(0, 50), offsets)
        assertEquals((0..50).map { EventHandler.User("user-$it", "name-$it") }, users)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `roles query returns an empty list and logs server failures`() {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.Forbidden)) {
            val errors = withResponse(
                """{"success":false,"error":"invalid-role"}""",
                status = status,
                invokeClient = { assertTrue(it.getUsersByRole("bot").isEmpty()) },
            )
            assertTrue(errors.single().contains("invalid-role"))
        }
    }

    @Test
    fun `roles query rejects users without an ID or username`() {
        val errors = withResponse(
            """{"success":true,"users":[{"_id":"user-1"}]}""",
            invokeClient = { assertTrue(it.getUsersByRole("bot").isEmpty()) },
        )
        assertEquals(1, errors.size)
    }

    private fun withResponse(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        logLevel: Level = Level.DEBUG,
        verifyLogs: (List<LogEvent>) -> Unit = {},
        responseForRequest: ((HttpRequestData) -> String)? = null,
        invokeClient: (RestApiClient) -> Unit = { it.updateStatus() },
        verifyRequest: suspend (HttpRequestData) -> Unit = {},
    ): List<String> {
        val events = CopyOnWriteArrayList<LogEvent>()
        val appender = object : AbstractAppender(
            "status-test", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY,
        ) {
            override fun append(event: LogEvent) {
                events += event.toImmutable()
            }
        }
        val logger = LogManager.getLogger(RestApiClient::class.java) as Logger
        val oldLevel = logger.level
        val oldUserId = Bot.userId
        val oldToken = Bot.authToken
        appender.start()
        logger.addAppender(appender)
        logger.level = logLevel
        try {
            Bot.userId = "test-user"
            Bot.authToken = "test-token"
            val client = RestApiClient("chat.example.org") {
                MockEngine { request ->
                    verifyRequest(request)
                    respond(responseForRequest?.invoke(request) ?: response, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            invokeClient(client)
        } finally {
            Bot.userId = oldUserId
            Bot.authToken = oldToken
            logger.removeAppender(appender)
            logger.level = oldLevel
            appender.stop()
        }
        verifyLogs(events)
        return events.filter { it.level == Level.ERROR }.map { it.message.formattedMessage }
    }
}
