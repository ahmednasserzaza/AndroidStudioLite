package com.worldcup.androidstudiolite.feature.vcs

import com.worldcup.androidstudiolite.entities.Commit

data class VcsUiState(
    val projectName: String? = null,
    val busy: Boolean = false,
    val commits: List<Commit> = emptyList(),
    val commitMessage: String = "",
)

sealed interface VcsEffect {
    data object NavigateToProjects : VcsEffect
}

interface VcsInteractionListener {
    fun onLoadCommits()
    fun onCommitMessageChange(message: String)
    fun onCommitAndPush()
    fun onPull()
}
