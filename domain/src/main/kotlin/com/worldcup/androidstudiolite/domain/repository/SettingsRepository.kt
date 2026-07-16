package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.AgentConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun githubToken(): Flow<String>
    suspend fun setGithubToken(token: String)

    fun githubOwner(): Flow<String>
    suspend fun setGithubOwner(owner: String)

    fun privateRepos(): Flow<Boolean>
    suspend fun setPrivateRepos(enabled: Boolean)

    fun agentConfig(): Flow<AgentConfig?>
    suspend fun setAgentConfig(config: AgentConfig)

    suspend fun apiKey(providerId: String): String
    suspend fun setApiKey(providerId: String, key: String)

    fun onboardingDone(): Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)
}
