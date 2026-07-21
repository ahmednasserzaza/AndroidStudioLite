package com.worldcup.androidstudiolite.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.settings.CompleteOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val githubConnection: ObserveGitHubConnectionUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel<OnboardingScreenState, OnboardingScreenEffect>(OnboardingScreenState()),
    OnboardingInteractionListener {

    init {
        viewModelScope.launch {
            githubConnection.token().collect { token ->
                updateState { it.copy(githubConnected = token.isNotBlank()) }
            }
        }
    }

    override fun onConnectGitHub() {
        sendNewEffect(OnboardingScreenEffect.NavigateToGitHubSettings)
    }

    override fun onStartBuilding() = complete()

    override fun onSkip() = complete()

    private fun complete() {
        tryToExecute(
            callee = { completeOnboarding() },
            onSuccess = { sendNewEffect(OnboardingScreenEffect.NavigateToProjects) },
        )
    }
}
