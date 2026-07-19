package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.BranchStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.CommitDetail
import com.worldcup.androidstudiolite.entities.GitHubAccount
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.PullRequestInfo
import com.worldcup.androidstudiolite.entities.PushResult
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.WorkflowRun

interface GitHubRepository {
    suspend fun fetchAccount(token: String? = null): GitHubAccount

    suspend fun ensureRepo(owner: String, repo: String, private: Boolean = true)

    suspend fun pushProject(
        owner: String,
        project: Project,
        message: String,
        force: Boolean = false,
    ): PushResult

    suspend fun findRun(owner: String, repo: String, commitSha: String): WorkflowRun?
    suspend fun getRun(owner: String, repo: String, runId: Long): WorkflowRun
    suspend fun failedSteps(owner: String, repo: String, runId: Long): List<String>
    suspend fun errorLines(owner: String, repo: String, runId: Long): List<String>

    suspend fun downloadApkArtifact(owner: String, repo: String, runId: Long): String

    suspend fun pullProject(owner: String, project: Project): Int

    suspend fun listCommits(owner: String, repo: String, branch: String, limit: Int = 20): List<Commit>

    suspend fun listUserRepos(): List<RemoteRepo>

    suspend fun defaultBranch(owner: String, repo: String): String

    suspend fun listBranches(owner: String, repo: String): List<Branch>

    suspend fun createBranch(owner: String, repo: String, name: String, fromBranch: String)

    suspend fun deleteBranch(owner: String, repo: String, name: String)

    suspend fun compareBranches(owner: String, repo: String, base: String, head: String): BranchStatus

    suspend fun commitDetail(owner: String, repo: String, sha: String): CommitDetail

    suspend fun branchCheckStatuses(owner: String, repo: String, branch: String): Map<String, CheckStatus>

    suspend fun listPullRequests(owner: String, repo: String): List<PullRequestInfo>

    suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
    ): PullRequestInfo

    suspend fun mergePullRequest(owner: String, repo: String, number: Int)

    suspend fun fileContent(owner: String, repo: String, path: String, ref: String): ByteArray
}
