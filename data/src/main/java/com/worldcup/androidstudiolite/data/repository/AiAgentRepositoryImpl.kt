package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.remote.ai.AiAgentDataSource
import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.AiAgentRepository
import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import com.worldcup.androidstudiolite.entities.AgentConfig
import com.worldcup.androidstudiolite.entities.AiChunk
import com.worldcup.androidstudiolite.entities.AiFileContext
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiModel
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiAgentRepositoryImpl(
    private val dataSources: List<AiAgentDataSource>,
    private val settings: SettingsRepository,
) : AiAgentRepository {

    override fun availableProviders(): List<AiProviderDescriptor> =
        dataSources.map { it.descriptor }

    override suspend fun validateKey(providerId: String, apiKey: String) =
        sourceFor(providerId).validateKey(apiKey)

    override suspend fun listModels(providerId: String): List<AiModel> {
        val key = settings.apiKey(providerId)
        if (key.isBlank()) throw DomainException.Auth("Connect ${providerId} first")
        return sourceFor(providerId).listModels(key)
    }

    override fun chat(
        config: AgentConfig,
        history: List<AiMessage>,
        fileContext: AiFileContext?,
    ): Flow<AiChunk> = flow {
        val key = settings.apiKey(config.providerId)
        if (key.isBlank()) {
            throw DomainException.Auth("No API key stored for ${config.providerId}")
        }
        sourceFor(config.providerId)
            .chat(key, config.modelId, history, fileContext)
            .collect { emit(it) }
    }

    private fun sourceFor(providerId: String): AiAgentDataSource =
        dataSources.firstOrNull { it.descriptor.id == providerId }
            ?: throw DomainException.Validation("Unknown AI provider: $providerId")
}
