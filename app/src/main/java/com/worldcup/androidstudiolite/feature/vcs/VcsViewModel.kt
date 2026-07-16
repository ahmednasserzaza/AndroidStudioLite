package com.worldcup.androidstudiolite.feature.vcs

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.git.CommitAndPushUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitsUseCase
import com.worldcup.androidstudiolite.domain.git.PullProjectUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.launch

class VcsViewModel(
    private val getCommits: GetCommitsUseCase,
    private val commitAndPush: CommitAndPushUseCase,
    private val pullProject: PullProjectUseCase,
    private val saveFile: SaveFileUseCase,
    private val repairInfrastructure: RepairProjectInfrastructureUseCase,
    private val workspace: WorkspaceSession,
) : BaseViewModel<VcsUiState, VcsEffect>(VcsUiState()),
    VcsInteractionListener {

    init {
        viewModelScope.launch {
            workspace.currentProject.collect { project ->
                updateState { it.copy(projectName = project?.name) }
                if (project != null) onLoadCommits()
            }
        }
    }

    override fun onLoadCommits() {
        val project = workspace.currentProject.value ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = { getCommits(project) },
            onSuccess = { commits -> updateState { it.copy(busy = false, commits = commits) } },
            onError = { updateState { it.copy(busy = false) } },
        )
    }

    override fun onCommitMessageChange(message: String) {
        updateState { it.copy(commitMessage = message) }
    }

    override fun onCommitAndPush() {
        val project = workspace.currentProject.value ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = {
                workspace.openFiles.value.filter { it.dirty }.forEach {
                    saveFile(it.path, it.content)
                }
                workspace.setOpenFiles(workspace.openFiles.value.map { it.copy(dirty = false) })
                repairInfrastructure(project)
                commitAndPush(project, currentState().commitMessage)
            },
            onSuccess = { result ->
                updateState { it.copy(busy = false, commitMessage = "") }
                showSnackBar(
                    if (result.pushed) "Pushed to GitHub"
                    else "Nothing to push — already up to date",
                )
                onLoadCommits()
            },
            onError = { updateState { it.copy(busy = false) } },
        )
    }

    override fun onPull() {
        val project = workspace.currentProject.value ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = { pullProject(project) },
            onSuccess = { count ->
                updateState { it.copy(busy = false) }
                workspace.setOpenFiles(emptyList())
                workspace.setActiveFile(null)
                showSnackBar("Pulled $count files from GitHub")
            },
            onError = { updateState { it.copy(busy = false) } },
        )
    }
}
