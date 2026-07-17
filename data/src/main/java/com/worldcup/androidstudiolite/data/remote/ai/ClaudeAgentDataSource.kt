package com.worldcup.androidstudiolite.data.remote.ai

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.entities.AiChunk
import com.worldcup.androidstudiolite.entities.AiFileContext
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiModel
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor
import com.worldcup.androidstudiolite.entities.AiRole
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class ClaudeAgentDataSource(
    private val client: HttpClient,
) : AiAgentDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override val descriptor = AiProviderDescriptor(
        id = "claude",
        displayName = "Anthropic Claude",
        apiKeyUrl = "https://platform.claude.com/settings/keys",
        defaultModelId = "claude-opus-4-8",
    )

    override suspend fun validateKey(apiKey: String) {
        val response = client.get("$BASE/models?limit=1") {
            claudeHeaders(apiKey)
        }
        if (!response.status.isSuccess()) {
            val message = errorMessage(response)
            throw when (response.status.value) {
                401, 403 -> DomainException.Auth(message ?: "Claude rejected this API key")
                else -> DomainException.Ai(message ?: "Claude error ${response.status.value}")
            }
        }
    }

    override suspend fun listModels(apiKey: String): List<AiModel> {
        val response = client.get("$BASE/models?limit=50") {
            claudeHeaders(apiKey)
        }
        if (!response.status.isSuccess()) {
            throw DomainException.Ai(
                errorMessage(response)
                    ?: "Couldn't load Claude models (HTTP ${response.status.value})",
            )
        }
        val models = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["data"]?.jsonArray ?: return emptyList()
        return models.map { element ->
            val model = element.jsonObject
            val id = model.getValue("id").jsonPrimitive.content
            AiModel(
                id = id,
                displayName = model["display_name"]?.jsonPrimitive?.content ?: id,
            )
        }
    }

    override fun chat(
        apiKey: String,
        modelId: String,
        history: List<AiMessage>,
        fileContext: AiFileContext?,
    ): Flow<AiChunk> = flow {
        val body = buildJsonObject {
            put("model", modelId)
            put("max_tokens", 16_000)
            put("stream", true)
            put("system", assistantSystemPrompt(fileContext))
            putJsonArray("messages") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", if (message.role == AiRole.User) "user" else "assistant")
                            put("content", message.text)
                        },
                    )
                }
            }
        }

        client.preparePost("$BASE/messages") {
            claudeHeaders(apiKey)
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val message = errorMessage(response)
                throw when (response.status.value) {
                    401, 403 -> DomainException.Auth(message ?: "Claude rejected this API key")
                    429 -> DomainException.Ai(message ?: "Claude rate limit reached — try again shortly")
                    else -> DomainException.Ai(message ?: "Claude error ${response.status.value}")
                }
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val payload = line.removePrefix("data: ").trim()
                if (payload.isEmpty()) continue
                val event = runCatching { json.parseToJsonElement(payload).jsonObject }
                    .getOrNull() ?: continue
                when (event["type"]?.jsonPrimitive?.content) {
                    "content_block_delta" -> {
                        val delta = event["delta"]?.jsonObject ?: continue
                        if (delta["type"]?.jsonPrimitive?.content == "text_delta") {
                            val text = delta["text"]?.jsonPrimitive?.content
                            if (!text.isNullOrEmpty()) emit(AiChunk(text))
                        }
                    }
                    "error" -> {
                        val message = event["error"]?.jsonObject
                            ?.get("message")?.jsonPrimitive?.content
                        throw DomainException.Ai(message ?: "Claude stream error")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun io.ktor.client.request.HttpRequestBuilder.claudeHeaders(apiKey: String) {
        header(API_KEY_HEADER, apiKey)
        header(VERSION_HEADER, API_VERSION)
    }

    private suspend fun errorMessage(response: HttpResponse): String? = runCatching {
        json.parseToJsonElement(response.bodyAsText()).jsonObject["error"]
            ?.jsonObject?.get("message")?.jsonPrimitive?.content
    }.getOrNull()

    companion object {
        private const val BASE = "https://api.anthropic.com/v1"
        private const val API_KEY_HEADER = "x-api-key"
        private const val VERSION_HEADER = "anthropic-version"
        private const val API_VERSION = "2023-06-01"
    }
}
