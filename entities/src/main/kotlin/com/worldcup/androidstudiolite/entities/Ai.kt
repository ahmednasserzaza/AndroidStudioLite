package com.worldcup.androidstudiolite.entities

enum class AiRole { User, Assistant }

data class AiMessage(
    val role: AiRole,
    val text: String,
)

data class AiChunk(val text: String)

data class AiModel(
    val id: String,
    val displayName: String,
)

data class AiProviderDescriptor(
    val id: String,
    val displayName: String,
    val apiKeyUrl: String,
    val defaultModelId: String,
)

data class AgentConfig(
    val providerId: String,
    val modelId: String,
)

data class AiFileContext(
    val path: String,
    val content: String,
)
