package com.worldcup.androidstudiolite.feature.vcs

import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.BranchStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.CommitDetail
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.PullRequestInfo

data class VcsUiState(
    val projectName: String? = null,
    val branch: String = "",
    val busy: Boolean = false,
    val commits: List<Commit> = emptyList(),
    val commitMessage: String = "",
    val changes: List<FileChange> = emptyList(),
    val checkStatuses: Map<String, CheckStatus> = emptyMap(),
    val branchStatus: BranchStatus? = null,
    val branchSheetVisible: Boolean = false,
    val branchesLoading: Boolean = false,
    val branches: List<Branch> = emptyList(),
    val newBranchVisible: Boolean = false,
    val branchWorking: Boolean = false,
    val checkoutBlocked: String? = null,
    val commitDetail: CommitDetail? = null,
    val commitDetailLoading: Boolean = false,
    val pulls: List<PullRequestInfo> = emptyList(),
    val createPrVisible: Boolean = false,
    val prWorking: Boolean = false,
    val mergingPr: Int? = null,
    val pullBlocked: Boolean = false,
    val discardTarget: FileChange? = null,
)

sealed interface VcsEffect {
    data object NavigateToProjects : VcsEffect
    data object NavigateToEditor : VcsEffect
}

interface VcsInteractionListener {
    fun onRefresh()
    fun onCommitMessageChange(message: String)
    fun onCommitAndPush()
    fun onPull()
    fun onConfirmPull()
    fun onDismissPull()
    fun onShowBranches(show: Boolean)
    fun onSelectBranch(name: String)
    fun onConfirmCheckout()
    fun onDismissCheckout()
    fun onShowNewBranch(show: Boolean)
    fun onCreateBranch(name: String)
    fun onDeleteBranch(name: String)
    fun onOpenCommit(sha: String)
    fun onDismissCommit()
    fun onOpenChange(change: FileChange)
    fun onRequestDiscard(change: FileChange)
    fun onConfirmDiscard()
    fun onDismissDiscard()
    fun onShowCreatePr(show: Boolean)
    fun onCreatePr(title: String)
    fun onMergePr(number: Int)
}
