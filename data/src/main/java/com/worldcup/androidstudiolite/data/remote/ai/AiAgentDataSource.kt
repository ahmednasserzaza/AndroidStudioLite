package com.worldcup.androidstudiolite.data.remote.ai

import com.worldcup.androidstudiolite.entities.AiChunk
import com.worldcup.androidstudiolite.entities.AiFileContext
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiModel
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor
import kotlinx.coroutines.flow.Flow

interface AiAgentDataSource {
    val descriptor: AiProviderDescriptor

    suspend fun validateKey(apiKey: String)

    suspend fun listModels(apiKey: String): List<AiModel>

    fun chat(
        apiKey: String,
        modelId: String,
        history: List<AiMessage>,
        fileContext: AiFileContext?,
    ): Flow<AiChunk>
}

internal fun assistantSystemPrompt(fileContext: AiFileContext?): String = buildString {
    append(
        "You are the AI assistant inside Android Studio Lite, a mobile IDE " +
            "for building Android apps with Kotlin and Jetpack Compose. " +
            "Help the user write, fix, and understand code. " +
            "Keep answers compact so they read well on a phone screen; " +
            "put code in fenced code blocks.",
    )
    if (fileContext != null) {
        append("\n\nThe user currently has this file open:\n")
        append("--- ${fileContext.path} ---\n")
        append(fileContext.content.take(24_000))
    }
}
