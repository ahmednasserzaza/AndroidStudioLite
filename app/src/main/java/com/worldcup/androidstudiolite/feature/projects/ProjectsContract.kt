package com.worldcup.androidstudiolite.feature.projects

import com.worldcup.androidstudiolite.entities.Project

data class ProjectsUiState(
    val loading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val githubConnected: Boolean = false,
    val showCreateDialog: Boolean = false,
    val creating: Boolean = false,
)

sealed interface ProjectsEffect {
    data object NavigateToEditor : ProjectsEffect
    data object NavigateToGitHubSettings : ProjectsEffect
}

interface ProjectsInteractionListener {
    fun onOpenProject(project: Project)
    fun onDeleteProject(project: Project)
    fun onShowCreateDialog(show: Boolean)
    fun onCreateProject(name: String, packageName: String)
    fun onConnectGitHub()
    fun onRefresh()
}
