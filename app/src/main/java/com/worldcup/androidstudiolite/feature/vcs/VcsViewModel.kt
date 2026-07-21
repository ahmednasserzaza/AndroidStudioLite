package com.worldcup.androidstudiolite.feature.vcs

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.git.CheckoutBranchUseCase
import com.worldcup.androidstudiolite.domain.git.CommitAndPushUseCase
import com.worldcup.androidstudiolite.domain.git.CreateBranchUseCase
import com.worldcup.androidstudiolite.domain.git.CreatePullRequestUseCase
import com.worldcup.androidstudiolite.domain.git.DeleteBranchUseCase
import com.worldcup.androidstudiolite.domain.git.DiscardFileChangeUseCase
import com.worldcup.androidstudiolite.domain.git.GetBranchChecksUseCase
import com.worldcup.androidstudiolite.domain.git.GetBranchStatusUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitDetailUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitsUseCase
import com.worldcup.androidstudiolite.domain.git.GetLocalChangesUseCase
import com.worldcup.androidstudiolite.domain.git.ListBranchesUseCase
import com.worldcup.androidstudiolite.domain.git.ListPullRequestsUseCase
import com.worldcup.androidstudiolite.domain.git.MergePullRequestUseCase
import com.worldcup.androidstudiolite.domain.git.PullProjectUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.OpenLocation
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.launch

class VcsViewModel(
    private val getCommits: GetCommitsUseCase,
    private val commitAndPush: CommitAndPushUseCase,
    private val pullProject: PullProjectUseCase,
    private val saveFile: SaveFileUseCase,
    private val repairInfrastructure: RepairProjectInfrastructureUseCase,
    private val listBranches: ListBranchesUseCase,
    private val createBranch: CreateBranchUseCase,
    private val checkoutBranch: CheckoutBranchUseCase,
    private val deleteBranch: DeleteBranchUseCase,
    private val getLocalChanges: GetLocalChangesUseCase,
    private val getBranchStatus: GetBranchStatusUseCase,
    private val getCommitDetail: GetCommitDetailUseCase,
    private val getBranchChecks: GetBranchChecksUseCase,
    private val listPullRequests: ListPullRequestsUseCase,
    private val createPullRequest: CreatePullRequestUseCase,
    private val mergePullRequest: MergePullRequestUseCase,
    private val discardFileChange: DiscardFileChangeUseCase,
    private val workspace: WorkspaceSession,
) : BaseViewModel<VcsScreenState, VcsScreenEffect>(VcsScreenState()),
    VcsInteractionListener {

    init {
        viewModelScope.launch {
            workspace.currentProject.collect { project ->
                updateState {
                    it.copy(
                        projectName = project?.name,
                        branch = project?.branch.orEmpty(),
                    )
                }
                if (project != null) onRefresh()
            }
        }
    }

    private fun project(): Project? = workspace.currentProject.value

    override fun onRefresh() {
        loadChanges()
        loadRemote()
    }

    private fun loadChanges() {
        val project = project() ?: return
        tryToExecute(
            callee = { flushDirtyTabs(); getLocalChanges(project) },
            onSuccess = { changes -> updateState { it.copy(changes = changes) } },
        )
    }

    private fun loadRemote() {
        val project = project() ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = { getCommits(project) },
            onSuccess = { commits -> updateState { it.copy(busy = false, commits = commits) } },
            onError = { updateState { it.copy(busy = false) } },
        )
        tryToExecute(
            callee = { getBranchChecks(project) },
            onSuccess = { checks -> updateState { it.copy(checkStatuses = checks) } },
            onError = {},
        )
        tryToExecute(
            callee = { getBranchStatus(project) },
            onSuccess = { status -> updateState { it.copy(branchStatus = status) } },
            onError = {},
        )
        tryToExecute(
            callee = { listPullRequests(project) },
            onSuccess = { pulls -> updateState { it.copy(pulls = pulls) } },
            onError = {},
        )
    }

    private suspend fun flushDirtyTabs() {
        val dirty = workspace.openFiles.value.filter { it.dirty }
        dirty.forEach { saveFile(it.path, it.content) }
        if (dirty.isNotEmpty()) {
            workspace.setOpenFiles(workspace.openFiles.value.map { it.copy(dirty = false) })
        }
    }

    override fun onCommitMessageChange(message: String) {
        updateState { it.copy(commitMessage = message) }
    }

    override fun onCommitAndPush() {
        val project = project() ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = {
                flushDirtyTabs()
                repairInfrastructure(project)
                commitAndPush(project, currentState().commitMessage)
            },
            onSuccess = { result ->
                updateState { it.copy(busy = false, commitMessage = "") }
                showSnackBar(
                    if (result.pushed) "Pushed to ${project.branch}"
                    else "Nothing to push — already up to date",
                )
                onRefresh()
            },
            onError = { updateState { it.copy(busy = false) } },
        )
    }

    override fun onPull() {
        if (currentState().changes.isNotEmpty()) {
            updateState { it.copy(pullBlocked = true) }
        } else {
            doPull()
        }
    }

    override fun onConfirmPull() {
        updateState { it.copy(pullBlocked = false) }
        doPull()
    }

    override fun onDismissPull() {
        updateState { it.copy(pullBlocked = false) }
    }

    private fun doPull() {
        val project = project() ?: return
        updateState { it.copy(busy = true) }
        tryToExecute(
            callee = { pullProject(project) },
            onSuccess = { count ->
                updateState { it.copy(busy = false) }
                workspace.setOpenFiles(emptyList())
                workspace.setActiveFile(null)
                showSnackBar("Pulled $count files from ${project.branch}")
                onRefresh()
            },
            onError = { updateState { it.copy(busy = false) } },
        )
    }

    override fun onShowBranches(show: Boolean) {
        updateState { it.copy(branchSheetVisible = show) }
        if (!show) return
        val project = project() ?: return
        updateState { it.copy(branchesLoading = true) }
        tryToExecute(
            callee = { listBranches(project) },
            onSuccess = { branches ->
                updateState { it.copy(branchesLoading = false, branches = branches) }
            },
            onError = { updateState { it.copy(branchesLoading = false) } },
        )
    }

    override fun onSelectBranch(name: String) {
        val project = project() ?: return
        if (name == project.branch) {
            updateState { it.copy(branchSheetVisible = false) }
            return
        }
        if (currentState().changes.isNotEmpty()) {
            updateState { it.copy(checkoutBlocked = name) }
        } else {
            doCheckout(name)
        }
    }

    override fun onConfirmCheckout() {
        val target = currentState().checkoutBlocked ?: return
        updateState { it.copy(checkoutBlocked = null) }
        doCheckout(target)
    }

    override fun onDismissCheckout() {
        updateState { it.copy(checkoutBlocked = null) }
    }

    private fun doCheckout(name: String) {
        val project = project() ?: return
        updateState { it.copy(branchWorking = true) }
        tryToExecute(
            callee = { checkoutBranch(project, name) },
            onSuccess = { updated ->
                updateState {
                    it.copy(branchWorking = false, branchSheetVisible = false, branch = updated.branch)
                }
                workspace.openProject(updated)
                showSnackBar("Switched to ${updated.branch}")
            },
            onError = { updateState { it.copy(branchWorking = false) } },
        )
    }

    override fun onShowNewBranch(show: Boolean) {
        updateState { it.copy(newBranchVisible = show) }
    }

    override fun onCreateBranch(name: String) {
        val project = project() ?: return
        updateState { it.copy(branchWorking = true) }
        tryToExecute(
            callee = { createBranch(project, name) },
            onSuccess = { updated ->
                updateState {
                    it.copy(
                        branchWorking = false,
                        newBranchVisible = false,
                        branchSheetVisible = false,
                        branch = updated.branch,
                    )
                }
                workspace.openProject(updated)
                showSnackBar("Created ${updated.branch}")
            },
            onError = { updateState { it.copy(branchWorking = false) } },
        )
    }

    override fun onDeleteBranch(name: String) {
        val project = project() ?: return
        tryToExecute(
            callee = { deleteBranch(project, name) },
            onSuccess = {
                updateState { state ->
                    state.copy(branches = state.branches.filterNot { it.name == name })
                }
                showSnackBar("Deleted $name")
            },
        )
    }

    override fun onOpenCommit(sha: String) {
        val project = project() ?: return
        updateState { it.copy(commitDetailLoading = true) }
        tryToExecute(
            callee = { getCommitDetail(project, sha) },
            onSuccess = { detail ->
                updateState { it.copy(commitDetailLoading = false, commitDetail = detail) }
            },
            onError = { updateState { it.copy(commitDetailLoading = false) } },
        )
    }

    override fun onDismissCommit() {
        updateState { it.copy(commitDetail = null) }
    }

    override fun onOpenChange(change: FileChange) {
        workspace.requestOpen(OpenLocation(change.relativePath, 1))
        sendNewEffect(VcsScreenEffect.NavigateToEditor)
    }

    override fun onRequestDiscard(change: FileChange) {
        updateState { it.copy(discardTarget = change) }
    }

    override fun onConfirmDiscard() {
        val project = project() ?: return
        val change = currentState().discardTarget ?: return
        updateState { it.copy(discardTarget = null, busy = true) }
        tryToExecute(
            callee = { discardFileChange(project, change) },
            onSuccess = {
                updateState { it.copy(busy = false) }
                val path = "${project.path}/${change.relativePath}"
                workspace.setOpenFiles(workspace.openFiles.value.filterNot { it.path == path })
                if (workspace.activeFilePath.value == path) {
                    workspace.setActiveFile(workspace.openFiles.value.firstOrNull()?.path)
                }
                showSnackBar("Discarded ${change.relativePath.substringAfterLast('/')}")
                loadChanges()
            },
            onError = { updateState { it.copy(busy = false) } },
        )
    }

    override fun onDismissDiscard() {
        updateState { it.copy(discardTarget = null) }
    }

    override fun onShowCreatePr(show: Boolean) {
        updateState { it.copy(createPrVisible = show) }
    }

    override fun onCreatePr(title: String) {
        val project = project() ?: return
        updateState { it.copy(prWorking = true) }
        tryToExecute(
            callee = { createPullRequest(project, title) },
            onSuccess = { pr ->
                updateState {
                    it.copy(prWorking = false, createPrVisible = false, pulls = it.pulls + pr)
                }
                showSnackBar("Opened PR #${pr.number}")
            },
            onError = { updateState { it.copy(prWorking = false) } },
        )
    }

    override fun onMergePr(number: Int) {
        val project = project() ?: return
        updateState { it.copy(mergingPr = number) }
        tryToExecute(
            callee = { mergePullRequest(project, number) },
            onSuccess = {
                updateState { state ->
                    state.copy(mergingPr = null, pulls = state.pulls.filterNot { it.number == number })
                }
                showSnackBar("Merged PR #$number")
                loadRemote()
            },
            onError = { updateState { it.copy(mergingPr = null) } },
        )
    }
}
