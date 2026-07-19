package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.local.prefs.SettingsDataSource
import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val local: SettingsDataSource,
) : SettingsRepository {

    override fun githubToken(): Flow<String> =
        local.tokenVersion.map { local.githubToken }

    override suspend fun setGithubToken(token: String) {
        local.githubToken = token
        local.bumpTokenVersion()
    }

    override fun githubOwner(): Flow<String> = local.githubOwner

    override suspend fun setGithubOwner(owner: String) = local.setGithubOwner(owner)

    override fun privateRepos(): Flow<Boolean> = local.privateRepos

    override suspend fun setPrivateRepos(enabled: Boolean) = local.setPrivateRepos(enabled)

    override fun onboardingDone(): Flow<Boolean> = local.onboardingDone

    override suspend fun setOnboardingDone(done: Boolean) = local.setOnboardingDone(done)

    override fun lastProjectId(): Flow<String> = local.lastProjectId

    override suspend fun setLastProjectId(id: String) = local.setLastProjectId(id)
}
