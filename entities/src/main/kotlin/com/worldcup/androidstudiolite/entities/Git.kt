package com.worldcup.androidstudiolite.entities

data class GitHubAccount(val login: String)

data class PushResult(
    val commitSha: String,
    val pushed: Boolean,
)

data class Commit(
    val sha: String,
    val message: String,
    val date: String,
)

data class RemoteRepo(
    val name: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val description: String,
    val updatedAt: String,
)

data class Branch(
    val name: String,
    val isDefault: Boolean,
)

data class BranchStatus(
    val ahead: Int,
    val behind: Int,
)

enum class ChangeStatus { Added, Modified, Deleted }

data class FileChange(
    val relativePath: String,
    val status: ChangeStatus,
)

data class CommitFileDiff(
    val path: String,
    val status: String,
    val patch: String,
)

data class CommitDetail(
    val sha: String,
    val message: String,
    val date: String,
    val files: List<CommitFileDiff>,
)

data class PullRequestInfo(
    val number: Int,
    val title: String,
    val headBranch: String,
    val baseBranch: String,
    val htmlUrl: String,
)

enum class CheckStatus { None, Pending, Success, Failure }
