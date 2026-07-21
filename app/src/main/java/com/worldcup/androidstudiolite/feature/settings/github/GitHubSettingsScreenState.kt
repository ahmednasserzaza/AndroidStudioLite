package com.worldcup.androidstudiolite.feature.settings.github

data class GitHubSettingsScreenState(
    val connected: Boolean = false,
    val owner: String = "",
    val tokenInput: String = "",
    val verifying: Boolean = false,
    val error: String? = null,
    val privateRepos: Boolean = true,
)
