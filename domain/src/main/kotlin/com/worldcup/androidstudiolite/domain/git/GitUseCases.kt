package com.worldcup.androidstudiolite.domain.git

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.GitHubAccount
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.PushResult
import kotlinx.coroutines.flow.first

class ConnectGitHubUseCase(
    private val github: GitHubRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(token: String): GitHubAccount {
        if (token.isBlank()) throw DomainException.Validation("Token can't be empty")
        val account = github.fetchAccount(token.trim())
        settings.setGithubToken(token.trim())
        settings.setGithubOwner(account.login)
        return account
    }
}

class EnsureOwnerUseCase(
    private val github: GitHubRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(): String {
        val existing = settings.githubOwner().first()
        if (existing.isNotBlank()) return existing
        val login = github.fetchAccount().login
        settings.setGithubOwner(login)
        return login
    }
}

class GetCommitsUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): List<Commit> =
        github.listCommits(ensureOwner(), project.repoName)
}

class CommitAndPushUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, message: String): PushResult {
        val owner = ensureOwner()
        github.ensureRepo(owner, project.repoName, project.isPrivate)
        return github.pushProject(
            owner = owner,
            project = project,
            message = message.ifBlank { "Update from Android Studio Lite" },
        )
    }
}

class PullProjectUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): Int =
        github.pullProject(ensureOwner(), project)
}
