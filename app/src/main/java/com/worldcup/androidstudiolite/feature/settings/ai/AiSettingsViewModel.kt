package com.worldcup.androidstudiolite.feature.settings.ai

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.ConnectAiProviderUseCase
import com.worldcup.androidstudiolite.domain.ai.GetAiModelsUseCase
import com.worldcup.androidstudiolite.domain.ai.GetAiProvidersUseCase
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.ai.SelectAiModelUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AiSettingsViewModel(
    private val getProviders: GetAiProvidersUseCase,
    private val connectProvider: ConnectAiProviderUseCase,
    private val getModels: GetAiModelsUseCase,
    private val selectModel: SelectAiModelUseCase,
    private val observeAgentConfig: ObserveAgentConfigUseCase,
) : BaseViewModel<AiSettingsUiState, AiSettingsEffect>(AiSettingsUiState()),
    AiSettingsInteractionListener {

    private var validateJob: Job? = null

    init {
        val providers = getProviders()
        updateState {
            it.copy(providers = providers, selectedProviderId = providers.firstOrNull()?.id)
        }
        viewModelScope.launch {
            observeAgentConfig().collect { config ->
                updateState {
                    it.copy(
                        connectedProviderId = config?.providerId,
                        activeModelId = config?.modelId,
                        selectedProviderId = config?.providerId ?: it.selectedProviderId,
                    )
                }
                if (config != null) loadModels(config.providerId)
            }
        }
    }

    override fun onSelectProvider(providerId: String) {
        updateState {
            it.copy(
                selectedProviderId = providerId,
                keyInput = "",
                keyStatus = KeyStatus.Idle,
                keyError = null,
            )
        }
    }

    override fun onKeyChange(key: String) {
        updateState { it.copy(keyInput = key, keyError = null) }
        validateJob?.cancel()
        if (key.isBlank()) {
            updateState { it.copy(keyStatus = KeyStatus.Idle) }
            return
        }
        updateState { it.copy(keyStatus = KeyStatus.Checking) }
        validateJob = viewModelScope.launch {
            delay(700)
            onConnect()
        }
    }

    override fun onConnect() {
        val providerId = currentState().selectedProviderId ?: return
        val key = currentState().keyInput
        if (key.isBlank()) return
        updateState { it.copy(keyStatus = KeyStatus.Checking) }
        tryToExecute(
            callee = { connectProvider(providerId, key) },
            onSuccess = {
                updateState { it.copy(keyStatus = KeyStatus.Valid, keyInput = "") }
                showSnackBar("Connected — models loaded")
                sendNewEffect(AiSettingsEffect.Connected)
            },
            onError = { error ->
                updateState {
                    it.copy(
                        keyStatus = KeyStatus.Invalid,
                        keyError = error.message ?: "This key was rejected",
                    )
                }
            },
        )
    }

    private fun loadModels(providerId: String) {
        updateState { it.copy(loadingModels = true) }
        tryToExecute(
            callee = { getModels(providerId) },
            onSuccess = { models ->
                updateState { it.copy(loadingModels = false, models = models) }
            },
            onError = { updateState { it.copy(loadingModels = false) } },
        )
    }

    override fun onSelectModel(modelId: String) {
        tryToExecute(
            callee = { selectModel(modelId) },
            onSuccess = { showSnackBar("Model set to $modelId") },
        )
    }
}
