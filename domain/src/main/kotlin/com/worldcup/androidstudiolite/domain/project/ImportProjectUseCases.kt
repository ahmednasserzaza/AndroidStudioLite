package com.worldcup.androidstudiolite.domain.project

import com.worldcup.androidstudiolite.domain.git.EnsureOwnerUseCase
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo

class ListImportableReposUseCase(
    private val github: GitHubRepository,
    private val projects: ProjectRepository,
) {
    suspend operator fun invoke(): List<RemoteRepo> {
        val existing = projects.listProjects().map { it.repoName }.toSet()
        return github.listUserRepos().filter { it.name !in existing }
    }
}

class ImportRepoUseCase(
    private val github: GitHubRepository,
    private val projects: ProjectRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(repo: RemoteRepo): Project {
        val owner = ensureOwner()
        val shell = projects.createImportShell(repo)
        try {
            github.pullProject(owner, shell)
            val project = projects.finalizeImport(shell)
            projects.repairInfrastructure(project)
            return project
        } catch (e: Exception) {
            projects.deleteProject(shell)
            throw e
        }
    }
}
