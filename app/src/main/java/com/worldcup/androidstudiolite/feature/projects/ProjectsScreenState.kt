package com.worldcup.androidstudiolite.feature.projects

import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo

data class ProjectsScreenState(
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
