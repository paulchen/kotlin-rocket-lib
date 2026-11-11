package at.rueckgr.kotlin.rocketbot.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager

internal val RestApiLogging = createClientPlugin("RestApiLogging") {
    val logger = LogManager.getLogger(RestApiClient::class.java)
    val mapper = jacksonObjectMapper()

    fun singleLine(body: String): String {
        if (body.isEmpty()) return "<empty>"
        val text = try {
            mapper.readTree(body)?.toString() ?: body
        } catch (_: JsonProcessingException) {
            body
        }
        return text.replace("\r", "\\r").replace("\n", "\\n")
    }

    on(Send) { request ->
        if (!logger.isDebugEnabled) return@on proceed(request)

        val content = request.body as OutgoingContent
        val bytes = content.bodyBytes()
        // Replay the serialized bytes so logging does not consume streaming request bodies.
        if (content !is OutgoingContent.NoContent) {
            request.setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = content.contentType
                override val contentLength = bytes.size.toLong()
                override val headers = content.headers
                override fun bytes() = bytes
            })
        }
        val method = request.method.value
        val endpoint = Url(request.url).toString()
        val body = bytes.toString(content.contentType?.charset() ?: Charsets.UTF_8)
        logger.debug("Request {} {} body={}", method, endpoint, singleLine(body))

        // Return a saved call so reading the response for logging does not consume it.
        val call = proceed(request).save()
        logger.debug(
            "Response {} {} code={} body={}",
            method, endpoint, call.response.status.value, singleLine(call.response.bodyAsText()),
        )
        call
    }
}

private suspend fun OutgoingContent.bodyBytes(): ByteArray = when (this) {
    is OutgoingContent.ContentWrapper -> delegate().bodyBytes()
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.ReadChannelContent -> readFrom().toByteArray()
    is OutgoingContent.WriteChannelContent -> coroutineScope {
        val channel = ByteChannel()
        launch {
            try {
                writeTo(channel)
            } finally {
                channel.close()
            }
        }
        channel.toByteArray()
    }
    is OutgoingContent.NoContent -> byteArrayOf()
    is OutgoingContent.ProtocolUpgrade -> error("Protocol upgrades are not supported by REST logging")
}
