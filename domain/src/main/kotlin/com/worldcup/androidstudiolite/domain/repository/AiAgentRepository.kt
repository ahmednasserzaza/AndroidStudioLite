package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.AgentConfig
import com.worldcup.androidstudiolite.entities.AiChunk
import com.worldcup.androidstudiolite.entities.AiFileContext
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiModel
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor
import kotlinx.coroutines.flow.Flow

interface AiAgentRepository {
    fun availableProviders(): List<AiProviderDescriptor>

    suspend fun validateKey(providerId: String, apiKey: String)

    suspend fun listModels(providerId: String): List<AiModel>

    fun chat(
        config: AgentConfig,
        history: List<AiMessage>,
        fileContext: AiFileContext?,
    ): Flow<AiChunk>
}
