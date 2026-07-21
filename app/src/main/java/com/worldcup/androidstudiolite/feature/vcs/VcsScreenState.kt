package com.worldcup.androidstudiolite.feature.vcs

import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.BranchStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.CommitDetail
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.PullRequestInfo

data class VcsScreenState(
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
