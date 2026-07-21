package com.worldcup.androidstudiolite.feature.vcs

import com.worldcup.androidstudiolite.entities.FileChange

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
