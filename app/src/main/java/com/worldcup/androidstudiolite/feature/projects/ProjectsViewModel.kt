package com.worldcup.androidstudiolite.feature.projects

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.domain.project.DeleteProjectUseCase
import com.worldcup.androidstudiolite.domain.project.GetProjectsUseCase
import com.worldcup.androidstudiolite.domain.project.ImportRepoUseCase
import com.worldcup.androidstudiolite.domain.project.ListImportableReposUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.domain.settings.ObservePrivateReposUseCase
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val getProjects: GetProjectsUseCase,
    private val createProject: CreateProjectUseCase,
    private val deleteProject: DeleteProjectUseCase,
    private val listImportableRepos: ListImportableReposUseCase,
    private val importRepo: ImportRepoUseCase,
    private val githubConnection: ObserveGitHubConnectionUseCase,
    private val observePrivateRepos: ObservePrivateReposUseCase,
    private val workspace: WorkspaceSession,
) : BaseViewModel<ProjectsUiState, ProjectsEffect>(ProjectsUiState()),
    ProjectsInteractionListener {

    init {
        onRefresh()
        viewModelScope.launch {
            githubConnection.token().collect { token ->
                updateState { it.copy(githubConnected = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            observePrivateRepos().collect { private ->
                updateState { it.copy(defaultPrivate = private) }
            }
        }
    }

    override fun onRefresh() {
        tryToExecute(
            callee = { getProjects() },
            onSuccess = { projects ->
                updateState { it.copy(loading = false, projects = projects) }
            },
            onError = { updateState { it.copy(loading = false) } },
        )
    }

    override fun onOpenProject(project: Project) {
        workspace.openProject(project)
        sendNewEffect(ProjectsEffect.NavigateToEditor)
    }

    override fun onRequestDeleteProject(project: Project) {
        updateState { it.copy(confirmDelete = project) }
    }

    override fun onDismissDeleteProject() {
        updateState { it.copy(confirmDelete = null) }
    }

    override fun onDeleteProject(project: Project) {
        updateState { it.copy(confirmDelete = null) }
        tryToExecute(
            callee = { deleteProject(project) },
            onSuccess = {
                if (workspace.currentProject.value?.id == project.id) workspace.closeProject()
                showSnackBar("Deleted ${project.name}")
                onRefresh()
            },
        )
    }

    override fun onShowCreateDialog(show: Boolean) {
        updateState { it.copy(showCreateDialog = show) }
    }

    override fun onCreateProject(name: String, packageName: String, isPrivate: Boolean) {
        updateState { it.copy(creating = true) }
        tryToExecute(
            callee = { createProject(name, packageName, isPrivate) },
            onSuccess = { project ->
                updateState { it.copy(creating = false, showCreateDialog = false) }
                onRefresh()
                onOpenProject(project)
            },
            onError = { updateState { it.copy(creating = false) } },
        )
    }

    override fun onShowImportDialog(show: Boolean) {
        updateState { it.copy(showImportDialog = show, importingRepo = null) }
        if (!show) return
        updateState { it.copy(loadingRepos = true, importableRepos = emptyList()) }
        tryToExecute(
            callee = { listImportableRepos() },
            onSuccess = { repos ->
                updateState { it.copy(loadingRepos = false, importableRepos = repos) }
            },
            onError = { updateState { it.copy(loadingRepos = false) } },
        )
    }

    override fun onImportRepo(repo: RemoteRepo) {
        if (currentState().importingRepo != null) return
        updateState { it.copy(importingRepo = repo.name) }
        tryToExecute(
            callee = { importRepo(repo) },
            onSuccess = { project ->
                updateState { it.copy(importingRepo = null, showImportDialog = false) }
                showSnackBar("Imported ${repo.name}")
                onRefresh()
                onOpenProject(project)
            },
            onError = { updateState { it.copy(importingRepo = null) } },
        )
    }

    override fun onConnectGitHub() = sendNewEffect(ProjectsEffect.NavigateToGitHubSettings)
}
