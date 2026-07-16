package com.worldcup.androidstudiolite.domain.settings

import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGitHubConnectionUseCase(private val settings: SettingsRepository) {
    data class State(val connected: Boolean, val owner: String)

    fun token(): Flow<String> = settings.githubToken()
    fun owner(): Flow<String> = settings.githubOwner()
}

class DisconnectGitHubUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke() {
        settings.setGithubToken("")
        settings.setGithubOwner("")
    }
}

class ObservePrivateReposUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = settings.privateRepos()
}

class SetPrivateReposUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = settings.setPrivateRepos(enabled)
}

class ObserveOnboardingUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = settings.onboardingDone()
}

class CompleteOnboardingUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke() = settings.setOnboardingDone(true)
}
