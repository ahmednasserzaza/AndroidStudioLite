package com.worldcup.androidstudiolite.feature.settings

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.GetAiProvidersUseCase
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import kotlinx.coroutines.launch

data class SettingsUiState(
    val githubConnected: Boolean = false,
    val githubOwner: String = "",
    val agentLabel: String? = null,
)

class SettingsViewModel(
    private val githubConnection: ObserveGitHubConnectionUseCase,
    private val observeAgentConfig: ObserveAgentConfigUseCase,
    private val getProviders: GetAiProvidersUseCase,
) : BaseViewModel<SettingsUiState, Unit>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            githubConnection.token().collect { token ->
                updateState { it.copy(githubConnected = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            githubConnection.owner().collect { owner ->
                updateState { it.copy(githubOwner = owner) }
            }
        }
        viewModelScope.launch {
            observeAgentConfig().collect { config ->
                val label = config?.let { c ->
                    val provider = getProviders().firstOrNull { it.id == c.providerId }
                    "${provider?.displayName ?: c.providerId} · ${c.modelId}"
                }
                updateState { it.copy(agentLabel = label) }
            }
        }
    }
}
