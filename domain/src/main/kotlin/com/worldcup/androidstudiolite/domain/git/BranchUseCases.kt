package com.worldcup.androidstudiolite.domain.git

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.BranchStatus
import com.worldcup.androidstudiolite.entities.ChangeStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.CommitDetail
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.PullRequestInfo

class ListBranchesUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): List<Branch> =
        github.listBranches(ensureOwner(), project.repoName)
}

class CreateBranchUseCase(
    private val github: GitHubRepository,
    private val projects: ProjectRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, name: String): Project {
        val trimmed = name.trim()
        if (!trimmed.matches(BRANCH_NAME_REGEX) || trimmed.startsWith("-") ||
            trimmed.contains("..") || trimmed.endsWith("/") || trimmed.endsWith(".lock")
        ) {
            throw DomainException.Validation("\"$name\" is not a valid branch name")
        }
        github.createBranch(ensureOwner(), project.repoName, trimmed, project.branch)
        return projects.setBranch(project, trimmed)
    }

    companion object {
        private val BRANCH_NAME_REGEX = Regex("^[A-Za-z0-9._/-]+$")
    }
}

class CheckoutBranchUseCase(
    private val github: GitHubRepository,
    private val projects: ProjectRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, branch: String): Project {
        val owner = ensureOwner()
        projects.clearWorkingTree(project)
        val updated = projects.setBranch(project, branch)
        github.pullProject(owner, updated)
        projects.recordSynced(updated)
        return updated
    }
}

class DeleteBranchUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, branch: String) {
        if (branch == project.branch) {
            throw DomainException.Validation("Can't delete the branch you're on")
        }
        github.deleteBranch(ensureOwner(), project.repoName, branch)
    }
}

class GetLocalChangesUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(project: Project): List<FileChange> =
        projects.localChanges(project)
}

class GetBranchStatusUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): BranchStatus? {
        val owner = ensureOwner()
        val default = github.defaultBranch(owner, project.repoName)
        if (default == project.branch) return null
        return github.compareBranches(owner, project.repoName, default, project.branch)
    }
}

class GetCommitDetailUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, sha: String): CommitDetail =
        github.commitDetail(ensureOwner(), project.repoName, sha)
}

class GetBranchChecksUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): Map<String, CheckStatus> =
        github.branchCheckStatuses(ensureOwner(), project.repoName, project.branch)
}

class ListPullRequestsUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project): List<PullRequestInfo> =
        github.listPullRequests(ensureOwner(), project.repoName)
}

class CreatePullRequestUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, title: String): PullRequestInfo {
        if (title.isBlank()) throw DomainException.Validation("Title can't be empty")
        val owner = ensureOwner()
        val base = github.defaultBranch(owner, project.repoName)
        if (base == project.branch) {
            throw DomainException.Validation("Already on $base — create a branch first")
        }
        return github.createPullRequest(owner, project.repoName, title.trim(), project.branch, base)
    }
}

class MergePullRequestUseCase(
    private val github: GitHubRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, number: Int) =
        github.mergePullRequest(ensureOwner(), project.repoName, number)
}

class DiscardFileChangeUseCase(
    private val github: GitHubRepository,
    private val projects: ProjectRepository,
    private val ensureOwner: EnsureOwnerUseCase,
) {
    suspend operator fun invoke(project: Project, change: FileChange) {
        when (change.status) {
            ChangeStatus.Added -> projects.deleteFile(project, change.relativePath)
            else -> {
                val bytes = github.fileContent(
                    ensureOwner(),
                    project.repoName,
                    change.relativePath,
                    project.branch,
                )
                projects.restoreFile(project, change.relativePath, bytes)
            }
        }
    }
}
