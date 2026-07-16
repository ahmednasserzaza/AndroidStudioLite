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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GeminiAgentDataSource(
    private val client: HttpClient,
) : AiAgentDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override val descriptor = AiProviderDescriptor(
        id = "gemini",
        displayName = "Google Gemini",
        apiKeyUrl = "https://aistudio.google.com/apikey",
        defaultModelId = "gemini-2.5-flash",
    )

    override suspend fun validateKey(apiKey: String) {
        val response = client.get("$BASE/models?pageSize=1") {
            header(API_KEY_HEADER, apiKey)
        }
        if (!response.status.isSuccess()) {
            throw when (response.status.value) {
                400, 401, 403 -> DomainException.Auth("Gemini rejected this API key")
                else -> DomainException.Ai("Gemini error ${response.status.value}")
            }
        }
    }

    override suspend fun listModels(apiKey: String): List<AiModel> {
        val response = client.get("$BASE/models?pageSize=50") {
            header(API_KEY_HEADER, apiKey)
        }
        if (!response.status.isSuccess()) {
            throw DomainException.Ai("Couldn't load Gemini models (HTTP ${response.status.value})")
        }
        val models = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["models"]?.jsonArray ?: return emptyList()
        return models.mapNotNull { element ->
            val model = element.jsonObject
            val supported = model["supportedGenerationMethods"]?.jsonArray
                ?.map { it.jsonPrimitive.content } ?: emptyList()
            if ("generateContent" !in supported) return@mapNotNull null
            val id = model.getValue("name").jsonPrimitive.content.removePrefix("models/")
            AiModel(
                id = id,
                displayName = model["displayName"]?.jsonPrimitive?.content ?: id,
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
            putJsonObject("system_instruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", assistantSystemPrompt(fileContext)) })
                }
            }
            putJsonArray("contents") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", if (message.role == AiRole.User) "user" else "model")
                            putJsonArray("parts") {
                                add(buildJsonObject { put("text", message.text) })
                            }
                        },
                    )
                }
            }
        }

        client.preparePost("$BASE/models/$modelId:streamGenerateContent?alt=sse") {
            header(API_KEY_HEADER, apiKey)
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                val message = runCatching {
                    json.parseToJsonElement(text).jsonObject["error"]
                        ?.jsonObject?.get("message")?.jsonPrimitive?.content
                }.getOrNull()
                throw when (response.status.value) {
                    400, 401, 403 -> DomainException.Auth(message ?: "Gemini rejected this API key")
                    429 -> DomainException.Ai(message ?: "Gemini rate limit reached — try again shortly")
                    else -> DomainException.Ai(message ?: "Gemini error ${response.status.value}")
                }
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val payload = line.removePrefix("data: ").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue
                val text = runCatching {
                    json.parseToJsonElement(payload).jsonObject["candidates"]?.jsonArray
                        ?.firstOrNull()?.jsonObject?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                        ?.joinToString("") { part ->
                            part.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                        }
                }.getOrNull()
                if (!text.isNullOrEmpty()) emit(AiChunk(text))
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        private const val API_KEY_HEADER = "x-goog-api-key"
    }
}
