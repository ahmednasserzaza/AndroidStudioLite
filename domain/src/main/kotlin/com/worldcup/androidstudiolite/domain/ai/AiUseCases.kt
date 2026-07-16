package com.worldcup.androidstudiolite.domain.ai

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
import kotlinx.coroutines.flow.first

class GetAiProvidersUseCase(private val agents: AiAgentRepository) {
    operator fun invoke(): List<AiProviderDescriptor> = agents.availableProviders()
}

class ConnectAiProviderUseCase(
    private val agents: AiAgentRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(providerId: String, apiKey: String): AgentConfig {
        if (apiKey.isBlank()) throw DomainException.Validation("API key can't be empty")
        agents.validateKey(providerId, apiKey.trim())
        settings.setApiKey(providerId, apiKey.trim())
        val descriptor = agents.availableProviders().first { it.id == providerId }
        val existing = settings.agentConfig().first()
        val config = if (existing?.providerId == providerId) {
            existing
        } else {
            AgentConfig(providerId = providerId, modelId = descriptor.defaultModelId)
        }
        settings.setAgentConfig(config)
        return config
    }
}

class GetAiModelsUseCase(private val agents: AiAgentRepository) {
    suspend operator fun invoke(providerId: String): List<AiModel> = agents.listModels(providerId)
}

class ObserveAgentConfigUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<AgentConfig?> = settings.agentConfig()
}

class SelectAiModelUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(modelId: String) {
        val config = settings.agentConfig().first()
            ?: throw DomainException.Validation("Connect an AI provider first")
        settings.setAgentConfig(config.copy(modelId = modelId))
    }
}

class StreamAssistantReplyUseCase(
    private val agents: AiAgentRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(
        history: List<AiMessage>,
        fileContext: AiFileContext?,
    ): Flow<AiChunk> {
        val config = settings.agentConfig().first()
            ?: throw DomainException.Validation("Configure an AI agent in Settings first")
        return agents.chat(config, history, fileContext)
    }
}
