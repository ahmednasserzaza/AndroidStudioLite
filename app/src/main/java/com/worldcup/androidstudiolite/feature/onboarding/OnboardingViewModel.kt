package com.worldcup.androidstudiolite.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.settings.CompleteOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val githubConnected: Boolean = false,
    val aiConnected: Boolean = false,
)

class OnboardingViewModel(
    private val githubConnection: ObserveGitHubConnectionUseCase,
    private val observeAgentConfig: ObserveAgentConfigUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel<OnboardingUiState, Unit>(OnboardingUiState()) {

    init {
        viewModelScope.launch {
            githubConnection.token().collect { token ->
                updateState { it.copy(githubConnected = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            observeAgentConfig().collect { config ->
                updateState { it.copy(aiConnected = config != null) }
            }
        }
    }

    fun complete(onDone: () -> Unit) {
        tryToExecute(
            callee = { completeOnboarding() },
            onSuccess = { onDone() },
        )
    }
}
