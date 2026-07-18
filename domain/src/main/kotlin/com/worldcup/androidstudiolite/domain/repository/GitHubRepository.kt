package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.GitHubAccount
import com.worldcup.androidstudiolite.entities.Project
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

    suspend fun listCommits(owner: String, repo: String, limit: Int = 20): List<Commit>

    suspend fun listUserRepos(): List<RemoteRepo>
}
