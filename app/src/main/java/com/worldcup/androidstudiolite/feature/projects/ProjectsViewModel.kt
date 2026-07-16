package com.worldcup.androidstudiolite.feature.projects

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.GetAiProvidersUseCase
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.domain.project.DeleteProjectUseCase
import com.worldcup.androidstudiolite.domain.project.GetProjectsUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val getProjects: GetProjectsUseCase,
    private val createProject: CreateProjectUseCase,
    private val deleteProject: DeleteProjectUseCase,
    private val githubConnection: ObserveGitHubConnectionUseCase,
    private val observeAgentConfig: ObserveAgentConfigUseCase,
    private val getAiProviders: GetAiProvidersUseCase,
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
            observeAgentConfig().collect { config ->
                val providerName = config?.let { c ->
                    getAiProviders().firstOrNull { it.id == c.providerId }?.displayName
                }
                updateState { it.copy(aiConnected = config != null, aiProviderName = providerName) }
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

    override fun onDeleteProject(project: Project) {
        tryToExecute(
            callee = { deleteProject(project) },
            onSuccess = {
                if (workspace.currentProject.value?.id == project.id) workspace.closeProject()
                onRefresh()
            },
        )
    }

    override fun onShowCreateDialog(show: Boolean) {
        updateState { it.copy(showCreateDialog = show) }
    }

    override fun onCreateProject(name: String, packageName: String) {
        updateState { it.copy(creating = true) }
        tryToExecute(
            callee = { createProject(name, packageName) },
            onSuccess = { project ->
                updateState { it.copy(creating = false, showCreateDialog = false) }
                onRefresh()
                onOpenProject(project)
            },
            onError = { updateState { it.copy(creating = false) } },
        )
    }

    override fun onConnectGitHub() = sendNewEffect(ProjectsEffect.NavigateToGitHubSettings)

    override fun onConfigureAi() = sendNewEffect(ProjectsEffect.NavigateToAiSettings)
}
