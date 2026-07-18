package com.worldcup.androidstudiolite.feature.projects

import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo

data class ProjectsUiState(
    val loading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val githubConnected: Boolean = false,
    val showCreateDialog: Boolean = false,
    val creating: Boolean = false,
    val defaultPrivate: Boolean = true,
    val showImportDialog: Boolean = false,
    val loadingRepos: Boolean = false,
    val importableRepos: List<RemoteRepo> = emptyList(),
    val importingRepo: String? = null,
    val confirmDelete: Project? = null,
)

sealed interface ProjectsEffect {
    data object NavigateToEditor : ProjectsEffect
    data object NavigateToGitHubSettings : ProjectsEffect
}

interface ProjectsInteractionListener {
    fun onOpenProject(project: Project)
    fun onRequestDeleteProject(project: Project)
    fun onDismissDeleteProject()
    fun onDeleteProject(project: Project)
    fun onShowCreateDialog(show: Boolean)
    fun onCreateProject(name: String, packageName: String, isPrivate: Boolean)
    fun onShowImportDialog(show: Boolean)
    fun onImportRepo(repo: RemoteRepo)
    fun onConnectGitHub()
    fun onRefresh()
}
