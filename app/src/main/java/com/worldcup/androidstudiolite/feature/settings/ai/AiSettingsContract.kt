package com.worldcup.androidstudiolite.feature.settings.ai

import com.worldcup.androidstudiolite.entities.AiModel
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor

enum class KeyStatus { Idle, Checking, Valid, Invalid }

data class AiSettingsUiState(
    val providers: List<AiProviderDescriptor> = emptyList(),
    val selectedProviderId: String? = null,
    val connectedProviderId: String? = null,
    val activeModelId: String? = null,
    val keyInput: String = "",
    val keyStatus: KeyStatus = KeyStatus.Idle,
    val keyError: String? = null,
    val models: List<AiModel> = emptyList(),
    val loadingModels: Boolean = false,
)

sealed interface AiSettingsEffect {
    data object Connected : AiSettingsEffect
}

interface AiSettingsInteractionListener {
    fun onSelectProvider(providerId: String)
    fun onKeyChange(key: String)
    fun onConnect()
    fun onSelectModel(modelId: String)
}
