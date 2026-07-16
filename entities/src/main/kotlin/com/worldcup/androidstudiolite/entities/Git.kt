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
