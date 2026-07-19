package com.worldcup.androidstudiolite.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun githubToken(): Flow<String>
    suspend fun setGithubToken(token: String)

    fun githubOwner(): Flow<String>
    suspend fun setGithubOwner(owner: String)

    fun privateRepos(): Flow<Boolean>
    suspend fun setPrivateRepos(enabled: Boolean)

    fun onboardingDone(): Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    fun lastProjectId(): Flow<String>
    suspend fun setLastProjectId(id: String)
}
