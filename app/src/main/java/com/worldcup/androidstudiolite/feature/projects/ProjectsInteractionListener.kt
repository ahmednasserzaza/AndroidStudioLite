package com.worldcup.androidstudiolite.feature.projects

import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo

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
