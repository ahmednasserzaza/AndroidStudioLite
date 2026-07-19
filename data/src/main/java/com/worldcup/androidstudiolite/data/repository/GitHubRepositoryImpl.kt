package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.local.fs.ProjectFileSystemDataSource
import com.worldcup.androidstudiolite.data.local.templates.ProjectTemplates
import com.worldcup.androidstudiolite.data.remote.github.GitHubDataSource
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.BranchStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.CommitDetail
import com.worldcup.androidstudiolite.entities.CommitFileDiff
import com.worldcup.androidstudiolite.entities.GitHubAccount
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.PullRequestInfo
import com.worldcup.androidstudiolite.entities.PushResult
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.RunConclusion
import com.worldcup.androidstudiolite.entities.RunStatus
import com.worldcup.androidstudiolite.entities.WorkflowRun
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
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
            excludeNames = setOf(ProjectTemplates.META_FILE, ProjectFileSystemDataSource.SYNC_FILE),
            message = message,
            force = force,
            branch = project.branch,
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
        github.pullProject(owner, project.repoName, File(project.path), project.branch)

    override suspend fun listUserRepos(): List<RemoteRepo> =
        github.listUserRepos().map { element ->
            val repo = element.jsonObject
            RemoteRepo(
                name = repo.getValue("name").jsonPrimitive.content,
                isPrivate = repo["private"]?.jsonPrimitive?.content == "true",
                defaultBranch = repo["default_branch"]?.jsonPrimitive?.content ?: "main",
                description = repo["description"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = (repo["updated_at"]?.jsonPrimitive?.contentOrNull ?: "")
                    .replace("T", " ").removeSuffix("Z"),
            )
        }

    override suspend fun listCommits(owner: String, repo: String, branch: String, limit: Int): List<Commit> =
        github.listCommits(owner, repo, branch, limit).map { element ->
            val commit = element.jsonObject
            val inner = commit.getValue("commit").jsonObject
            Commit(
                sha = commit.getValue("sha").jsonPrimitive.content.take(7),
                message = inner.getValue("message").jsonPrimitive.content.lineSequence().first(),
                date = inner.getValue("author").jsonObject.getValue("date").jsonPrimitive.content
                    .replace("T", " ").removeSuffix("Z"),
            )
        }

    override suspend fun defaultBranch(owner: String, repo: String): String =
        github.getRepo(owner, repo)["default_branch"]?.jsonPrimitive?.contentOrNull ?: "main"

    override suspend fun listBranches(owner: String, repo: String): List<Branch> {
        val default = defaultBranch(owner, repo)
        return github.listBranches(owner, repo).map { element ->
            val name = element.jsonObject.getValue("name").jsonPrimitive.content
            Branch(name = name, isDefault = name == default)
        }.sortedWith(compareByDescending<Branch> { it.isDefault }.thenBy { it.name })
    }

    override suspend fun createBranch(owner: String, repo: String, name: String, fromBranch: String) {
        val sha = github.getBranchSha(owner, repo, fromBranch)
        github.createBranchRef(owner, repo, name, sha)
    }

    override suspend fun deleteBranch(owner: String, repo: String, name: String) =
        github.deleteBranchRef(owner, repo, name)

    override suspend fun compareBranches(
        owner: String,
        repo: String,
        base: String,
        head: String,
    ): BranchStatus {
        val comparison = github.compareBranches(owner, repo, base, head)
        return BranchStatus(
            ahead = comparison["ahead_by"]?.jsonPrimitive?.intOrNull ?: 0,
            behind = comparison["behind_by"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    override suspend fun commitDetail(owner: String, repo: String, sha: String): CommitDetail {
        val detail = github.getCommit(owner, repo, sha)
        val inner = detail.getValue("commit").jsonObject
        val files = detail["files"]?.jsonArray?.map { fileElement ->
            val file = fileElement.jsonObject
            CommitFileDiff(
                path = file.getValue("filename").jsonPrimitive.content,
                status = file["status"]?.jsonPrimitive?.contentOrNull ?: "",
                patch = file["patch"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        }.orEmpty()
        return CommitDetail(
            sha = detail.getValue("sha").jsonPrimitive.content.take(7),
            message = inner.getValue("message").jsonPrimitive.content,
            date = inner.getValue("author").jsonObject.getValue("date").jsonPrimitive.content
                .replace("T", " ").removeSuffix("Z"),
            files = files,
        )
    }

    override suspend fun branchCheckStatuses(
        owner: String,
        repo: String,
        branch: String,
    ): Map<String, CheckStatus> =
        github.listRunsForBranch(owner, repo, branch).mapNotNull { element ->
            val run = element.jsonObject
            val sha = run["head_sha"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val status = when {
                run["status"]?.jsonPrimitive?.contentOrNull != "completed" -> CheckStatus.Pending
                run["conclusion"]?.jsonPrimitive?.contentOrNull == "success" -> CheckStatus.Success
                else -> CheckStatus.Failure
            }
            sha.take(7) to status
        }.reversed().toMap()

    override suspend fun listPullRequests(owner: String, repo: String): List<PullRequestInfo> =
        github.listPulls(owner, repo).map { element -> element.jsonObject.toPullRequest() }

    override suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
    ): PullRequestInfo = github.createPull(owner, repo, title, head, base).toPullRequest()

    override suspend fun mergePullRequest(owner: String, repo: String, number: Int) =
        github.mergePull(owner, repo, number)

    override suspend fun fileContent(owner: String, repo: String, path: String, ref: String): ByteArray =
        github.getFileContent(owner, repo, path, ref)

    private fun JsonObject.toPullRequest(): PullRequestInfo = PullRequestInfo(
        number = getValue("number").jsonPrimitive.content.toInt(),
        title = getValue("title").jsonPrimitive.content,
        headBranch = getValue("head").jsonObject.getValue("ref").jsonPrimitive.content,
        baseBranch = getValue("base").jsonObject.getValue("ref").jsonPrimitive.content,
        htmlUrl = getValue("html_url").jsonPrimitive.content,
    )

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
