package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.local.fs.ProjectFileSystemDataSource
import com.worldcup.androidstudiolite.data.local.templates.ProjectTemplates
import com.worldcup.androidstudiolite.data.remote.github.GitHubDataSource
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.GitHubAccount
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.PushResult
import com.worldcup.androidstudiolite.entities.RunConclusion
import com.worldcup.androidstudiolite.entities.RunStatus
import com.worldcup.androidstudiolite.entities.WorkflowRun
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File

class GitHubRepositoryImpl(
    private val github: GitHubDataSource,
    private val fs: ProjectFileSystemDataSource,
) : GitHubRepository {

    override suspend fun fetchAccount(token: String?): GitHubAccount =
        GitHubAccount(github.fetchLogin(token))

    override suspend fun ensureRepo(owner: String, repo: String, private: Boolean) =
        github.ensureRepo(owner, repo, private)

    override suspend fun pushProject(
        owner: String,
        project: Project,
        message: String,
        force: Boolean,
    ): PushResult =
        github.pushProject(
            owner = owner,
            repo = project.repoName,
            projectDir = File(project.path),
            excludeName = ProjectTemplates.META_FILE,
            message = message,
            force = force,
        )

    override suspend fun findRun(owner: String, repo: String, commitSha: String): WorkflowRun? =
        github.findRun(owner, repo, commitSha)?.toRun()

    override suspend fun getRun(owner: String, repo: String, runId: Long): WorkflowRun =
        github.getRun(owner, repo, runId).toRun()

    override suspend fun failedSteps(owner: String, repo: String, runId: Long): List<String> {
        val jobs = github.listJobs(owner, repo, runId)
        val failed = mutableListOf<String>()
        for (element in jobs) {
            val job = element.jsonObject
            val steps = job["steps"] as? kotlinx.serialization.json.JsonArray ?: continue
            for (stepElement in steps) {
                val step = stepElement.jsonObject
                if (step["conclusion"]?.jsonPrimitive?.content == "failure") {
                    failed += "${job.getValue("name").jsonPrimitive.content} / " +
                        step.getValue("name").jsonPrimitive.content
                }
            }
        }
        return failed
    }

    override suspend fun errorLines(owner: String, repo: String, runId: Long): List<String> {
        val jobs = github.listJobs(owner, repo, runId)
        val failedJobId = jobs.firstNotNullOfOrNull { element ->
            val job = element.jsonObject
            if (job["conclusion"]?.jsonPrimitive?.content == "failure") {
                job.getValue("id").jsonPrimitive.longOrNull
            } else {
                null
            }
        } ?: return emptyList()

        val timestamp = Regex("^\\S+\\s")
        return github.readJobLog(owner, repo, failedJobId) { lines ->
            lines
                .map { it.replace(timestamp, "") }
                .filter {
                    it.startsWith("e: ") || it.startsWith("error:") || it.startsWith("FAILURE:") ||
                        (
                            it.startsWith("> ") && !it.startsWith("> Task") &&
                                !it.startsWith("> Run ") && !it.startsWith("> Get more") &&
                                !it.startsWith("> There were")
                            )
                }
                .map { it.replace("file:///home/runner/work/$repo/$repo/", "") }
                .filter { it.length > 3 }
                .take(12)
                .toList()
        } ?: emptyList()
    }

    override suspend fun downloadApkArtifact(owner: String, repo: String, runId: Long): String =
        github.downloadApkArtifact(owner, repo, runId, fs.apkCacheDir).absolutePath

    override suspend fun pullProject(owner: String, project: Project): Int =
        github.pullProject(owner, project.repoName, File(project.path))

    override suspend fun listCommits(owner: String, repo: String, limit: Int): List<Commit> =
        github.listCommits(owner, repo, limit).map { element ->
            val commit = element.jsonObject
            val inner = commit.getValue("commit").jsonObject
            Commit(
                sha = commit.getValue("sha").jsonPrimitive.content.take(7),
                message = inner.getValue("message").jsonPrimitive.content.lineSequence().first(),
                date = inner.getValue("author").jsonObject.getValue("date").jsonPrimitive.content
                    .replace("T", " ").removeSuffix("Z"),
            )
        }

    private fun JsonObject.toRun(): WorkflowRun = WorkflowRun(
        id = getValue("id").jsonPrimitive.content.toLong(),
        status = when (get("status")?.jsonPrimitive?.content) {
            "queued" -> RunStatus.Queued
            "in_progress" -> RunStatus.InProgress
            "completed" -> RunStatus.Completed
            else -> RunStatus.Unknown
        },
        conclusion = when (get("conclusion")?.jsonPrimitive?.content) {
            null -> null
            "success" -> RunConclusion.Success
            "failure" -> RunConclusion.Failure
            "cancelled" -> RunConclusion.Cancelled
            else -> RunConclusion.Other
        },
        htmlUrl = getValue("html_url").jsonPrimitive.content,
    )
}
