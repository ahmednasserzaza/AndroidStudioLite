package com.worldcup.androidstudiolite.feature.vcs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslDangerButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslChip
import com.worldcup.androidstudiolite.designsystem.components.dialogs.AslDialog
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslCircularProgress
import com.worldcup.androidstudiolite.designsystem.components.sheets.AslBottomSheet
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.entities.Branch
import com.worldcup.androidstudiolite.entities.ChangeStatus
import com.worldcup.androidstudiolite.entities.CheckStatus
import com.worldcup.androidstudiolite.entities.Commit
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.PullRequestInfo
import com.worldcup.androidstudiolite.feature.base.CollectEffects
import com.worldcup.androidstudiolite.feature.editor.ui.FileTypeBadge

@Composable
fun VcsScreen(
    viewModel: VcsViewModel,
    onNavigateToProjects: () -> Unit,
    onNavigateToEditor: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: VcsInteractionListener = viewModel

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            VcsEffect.NavigateToProjects -> onNavigateToProjects()
            VcsEffect.NavigateToEditor -> onNavigateToEditor()
        }
    }

    if (state.projectName == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(AslTheme.colors.background)
                .padding(AslTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AslIcon(AslIcons.GitBranch, size = 40.dp, tint = AslTheme.colors.onSurfaceVariant)
            AslText("No project open", style = AslTheme.typography.title)
            AslPrimaryButton("Go to Projects", onClick = onNavigateToProjects)
        }
        return
    }

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AslTopBar(
                title = "",
                titleLeading = { BranchChip(state, onClick = { listener.onShowBranches(true) }) },
                actions = {
                    AslIconButton(
                        AslIcons.Refresh,
                        onClick = listener::onRefresh,
                        contentDescription = "Refresh",
                    )
                },
            )

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = AslTheme.spacing.gutter,
                    vertical = AslTheme.spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
            ) {
                item { ChangesCard(state, listener) }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = AslTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AslText(
                            "Pull Requests",
                            style = AslTheme.typography.uiHeader,
                            modifier = Modifier.weight(1f),
                        )
                        AslTextButton("New PR", onClick = { listener.onShowCreatePr(true) })
                    }
                }
                if (state.pulls.isEmpty()) {
                    item {
                        AslText(
                            "No open pull requests.",
                            style = AslTheme.typography.uiLabelSmall,
                            color = AslTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                items(state.pulls, key = { "pr${it.number}" }) { pr ->
                    PullRequestRow(pr, state, listener)
                }

                item {
                    AslText(
                        "History",
                        style = AslTheme.typography.uiHeader,
                        modifier = Modifier.padding(top = AslTheme.spacing.md),
                    )
                }
                if (state.commits.isEmpty() && !state.busy) {
                    item {
                        AslText(
                            "No commits yet. Commit & Push will create the repository on GitHub.",
                            style = AslTheme.typography.uiBody,
                            color = AslTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = AslTheme.spacing.md),
                        )
                    }
                }
                items(state.commits, key = { it.sha }) { commit ->
                    CommitRow(commit, state.checkStatuses[commit.sha], listener)
                }
            }
        }

        BranchSheet(state, listener)

        AslSnackbarHost(snackBar)

        if (state.newBranchVisible) NewBranchDialog(state, listener)
        state.checkoutBlocked?.let { CheckoutBlockedDialog(it, state, listener) }
        if (state.pullBlocked) PullBlockedDialog(state, listener)
        state.discardTarget?.let { DiscardDialog(it, listener) }
        if (state.createPrVisible) CreatePrDialog(state, listener)
        if (state.commitDetail != null || state.commitDetailLoading) {
            CommitDetailDialog(state, listener)
        }
    }
}

@Composable
private fun BranchChip(state: VcsUiState, onClick: () -> Unit) {
    Row(
        Modifier
            .background(AslTheme.colors.surfaceContainerHigh, AslTheme.shapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AslIcon(AslIcons.GitBranch, size = 14.dp, tint = AslTheme.colors.primary)
        AslText(
            state.branch.ifEmpty { "…" },
            style = AslTheme.typography.uiHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.branchStatus?.let { status ->
            AslText(
                "↑${status.ahead} ↓${status.behind}",
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
            )
        }
        AslIcon(AslIcons.ExpandMore, size = 14.dp, tint = AslTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun ChangesCard(state: VcsUiState, listener: VcsInteractionListener) {
    AslCard(Modifier.fillMaxWidth(), contentPadding = AslTheme.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AslText(
                "Changes",
                style = AslTheme.typography.uiHeader,
                modifier = Modifier.weight(1f),
            )
            AslChip(
                if (state.changes.isEmpty()) "Up to date" else "${state.changes.size} pending",
            )
        }
        if (state.changes.isNotEmpty()) {
            Spacer(Modifier.height(AslTheme.spacing.sm))
            Column {
                state.changes.take(MAX_VISIBLE_CHANGES).forEach { change ->
                    ChangeRow(change, listener)
                }
                if (state.changes.size > MAX_VISIBLE_CHANGES) {
                    AslText(
                        "…and ${state.changes.size - MAX_VISIBLE_CHANGES} more",
                        style = AslTheme.typography.uiLabelSmall,
                        color = AslTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslTextField(
            value = state.commitMessage,
            onValueChange = listener::onCommitMessageChange,
            placeholder = "Commit message",
        )
        Spacer(Modifier.height(AslTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
            AslPrimaryButton(
                "Commit & Push",
                onClick = listener::onCommitAndPush,
                enabled = state.commitMessage.isNotBlank(),
                loading = state.busy,
                leadingIconRes = AslIcons.Upload,
                modifier = Modifier.weight(1f),
            )
            AslGhostButton(
                "Pull",
                onClick = listener::onPull,
                enabled = !state.busy,
                leadingIconRes = AslIcons.Download,
            )
        }
    }
}

@Composable
private fun ChangeRow(change: FileChange, listener: VcsInteractionListener) {
    val (label, color) = when (change.status) {
        ChangeStatus.Added -> "A" to AslTheme.colors.primary
        ChangeStatus.Modified -> "M" to AslTheme.colors.secondary
        ChangeStatus.Deleted -> "D" to AslTheme.colors.error
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = change.status != ChangeStatus.Deleted) {
                listener.onOpenChange(change)
            }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
    ) {
        AslText(label, style = AslTheme.typography.codeSmall, color = color)
        AslText(
            change.relativePath,
            style = AslTheme.typography.uiLabelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AslIconButton(
            AslIcons.Undo,
            onClick = { listener.onRequestDiscard(change) },
            tint = AslTheme.colors.onSurfaceVariant,
            contentDescription = "Discard ${change.relativePath}",
        )
    }
}

@Composable
private fun PullRequestRow(
    pr: PullRequestInfo,
    state: VcsUiState,
    listener: VcsInteractionListener,
) {
    AslCard(
        Modifier.fillMaxWidth(),
        color = AslTheme.colors.surfaceContainerLow,
        contentPadding = AslTheme.spacing.md,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                AslText(
                    "#${pr.number}  ${pr.title}",
                    style = AslTheme.typography.uiBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AslText(
                    "${pr.headBranch} → ${pr.baseBranch}",
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
            AslGhostButton(
                if (state.mergingPr == pr.number) "Merging…" else "Merge",
                onClick = { listener.onMergePr(pr.number) },
                enabled = state.mergingPr == null,
            )
        }
    }
}

@Composable
private fun CommitRow(
    commit: Commit,
    check: CheckStatus?,
    listener: VcsInteractionListener,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { listener.onOpenCommit(commit.sha) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    when (check) {
                        CheckStatus.Success -> AslTheme.colors.primary
                        CheckStatus.Failure -> AslTheme.colors.error
                        CheckStatus.Pending -> AslTheme.colors.tertiaryContainer
                        else -> AslTheme.colors.outlineVariant
                    },
                    AslTheme.shapes.full,
                ),
        )
        AslText(
            commit.sha,
            style = AslTheme.typography.codeSmall,
            color = AslTheme.colors.primary,
        )
        Column(Modifier.weight(1f)) {
            AslText(commit.message, style = AslTheme.typography.uiBody, maxLines = 1)
            AslText(
                commit.date,
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
            )
        }
        AslIcon(AslIcons.ChevronRight, size = 14.dp, tint = AslTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun BranchSheet(state: VcsUiState, listener: VcsInteractionListener) {
    AslBottomSheet(
        visible = state.branchSheetVisible,
        onDismissRequest = { if (!state.branchWorking) listener.onShowBranches(false) },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AslText(
                "Branches",
                style = AslTheme.typography.title,
                modifier = Modifier.weight(1f),
            )
            AslTextButton(
                "New branch",
                onClick = { listener.onShowNewBranch(true) },
                enabled = !state.branchWorking,
            )
        }
        Spacer(Modifier.height(AslTheme.spacing.sm))
        when {
            state.branchWorking -> Row(
                Modifier.fillMaxWidth().padding(vertical = AslTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AslCircularProgress(size = 20.dp)
                AslText("Syncing branch…", color = AslTheme.colors.onSurfaceVariant)
            }
            state.branchesLoading -> Box(
                Modifier.fillMaxWidth().padding(vertical = AslTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                AslCircularProgress()
            }
            else -> LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(state.branches, key = { it.name }) { branch ->
                    BranchRow(branch, state, listener)
                    AslHorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BranchRow(branch: Branch, state: VcsUiState, listener: VcsInteractionListener) {
    val current = branch.name == state.branch
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { listener.onSelectBranch(branch.name) }
            .padding(vertical = AslTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
    ) {
        AslIcon(
            AslIcons.GitBranch,
            size = 14.dp,
            tint = if (current) AslTheme.colors.primary else AslTheme.colors.onSurfaceVariant,
        )
        AslText(
            branch.name,
            style = AslTheme.typography.uiBody,
            color = if (current) AslTheme.colors.primary else AslTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (branch.isDefault) AslChip("default")
        if (current) {
            AslIcon(AslIcons.Check, size = 16.dp, tint = AslTheme.colors.primary)
        } else if (!branch.isDefault) {
            AslIconButton(
                AslIcons.Delete,
                onClick = { listener.onDeleteBranch(branch.name) },
                tint = AslTheme.colors.onSurfaceVariant,
                contentDescription = "Delete ${branch.name}",
            )
        }
    }
}

@Composable
private fun NewBranchDialog(state: VcsUiState, listener: VcsInteractionListener) {
    var name by remember { mutableStateOf("") }
    AslDialog(
        title = "New Branch",
        onDismissRequest = { if (!state.branchWorking) listener.onShowNewBranch(false) },
        buttons = {
            AslGhostButton(
                "Cancel",
                onClick = { listener.onShowNewBranch(false) },
                enabled = !state.branchWorking,
            )
            AslPrimaryButton(
                "Create",
                onClick = { listener.onCreateBranch(name) },
                enabled = name.isNotBlank(),
                loading = state.branchWorking,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
            AslTextField(
                value = name,
                onValueChange = { name = it },
                label = "Branch name",
                placeholder = "feature/my-change",
            )
            AslText(
                "Created from ${state.branch}, including your local changes.",
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckoutBlockedDialog(
    target: String,
    state: VcsUiState,
    listener: VcsInteractionListener,
) {
    AslDialog(
        title = "Switch to $target?",
        onDismissRequest = listener::onDismissCheckout,
        buttons = {
            AslGhostButton("Cancel", onClick = listener::onDismissCheckout)
            AslDangerButton("Discard & switch", onClick = listener::onConfirmCheckout)
        },
    ) {
        AslText(
            "You have ${state.changes.size} unpushed change(s) on ${state.branch}. " +
                "Switching branches will discard them. Commit & Push first to keep them.",
            color = AslTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PullBlockedDialog(state: VcsUiState, listener: VcsInteractionListener) {
    AslDialog(
        title = "Overwrite local changes?",
        onDismissRequest = listener::onDismissPull,
        buttons = {
            AslGhostButton("Cancel", onClick = listener::onDismissPull)
            AslDangerButton("Pull anyway", onClick = listener::onConfirmPull)
        },
    ) {
        AslText(
            "You have ${state.changes.size} unpushed change(s). Pulling replaces local " +
                "files with the version on ${state.branch}.",
            color = AslTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiscardDialog(change: FileChange, listener: VcsInteractionListener) {
    AslDialog(
        title = "Discard changes?",
        onDismissRequest = listener::onDismissDiscard,
        buttons = {
            AslGhostButton("Cancel", onClick = listener::onDismissDiscard)
            AslDangerButton("Discard", onClick = listener::onConfirmDiscard)
        },
    ) {
        AslText(
            when (change.status) {
                ChangeStatus.Added -> "${change.relativePath} will be deleted."
                ChangeStatus.Deleted -> "${change.relativePath} will be restored from the branch."
                ChangeStatus.Modified ->
                    "${change.relativePath} will be reverted to the version on the branch."
            },
            color = AslTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreatePrDialog(state: VcsUiState, listener: VcsInteractionListener) {
    var title by remember {
        mutableStateOf(state.commits.firstOrNull()?.message ?: "")
    }
    AslDialog(
        title = "New Pull Request",
        onDismissRequest = { if (!state.prWorking) listener.onShowCreatePr(false) },
        buttons = {
            AslGhostButton(
                "Cancel",
                onClick = { listener.onShowCreatePr(false) },
                enabled = !state.prWorking,
            )
            AslPrimaryButton(
                "Create",
                onClick = { listener.onCreatePr(title) },
                enabled = title.isNotBlank(),
                loading = state.prWorking,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
            AslTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
            )
            AslText(
                "From ${state.branch} into the default branch.",
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommitDetailDialog(state: VcsUiState, listener: VcsInteractionListener) {
    AslDialog(
        title = state.commitDetail?.let { "${it.sha}  ${it.message.lineSequence().first()}" }
            ?: "Loading…",
        onDismissRequest = listener::onDismissCommit,
        buttons = {
            AslGhostButton("Close", onClick = listener::onDismissCommit)
        },
    ) {
        val detail = state.commitDetail
        if (detail == null) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = AslTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                AslCircularProgress()
            }
            return@AslDialog
        }
        LazyColumn(Modifier.heightIn(max = 440.dp)) {
            items(detail.files, key = { it.path }) { file ->
                Column(Modifier.padding(bottom = AslTheme.spacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
                    ) {
                        FileTypeBadge(file.path.substringAfterLast('/'))
                        AslText(
                            file.path,
                            style = AslTheme.typography.uiLabelSmall,
                            color = AslTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        AslChip(file.status)
                    }
                    if (file.patch.isNotBlank()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    AslTheme.colors.surfaceContainerLowest,
                                    AslTheme.shapes.default,
                                )
                                .horizontalScroll(rememberScrollState())
                                .padding(AslTheme.spacing.sm),
                        ) {
                            file.patch.lineSequence().forEach { line ->
                                AslText(
                                    line,
                                    style = AslTheme.typography.codeSmall,
                                    color = when {
                                        line.startsWith("+") -> AslTheme.colors.primary
                                        line.startsWith("-") -> AslTheme.colors.error
                                        line.startsWith("@@") -> AslTheme.colors.secondary
                                        else -> AslTheme.colors.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_VISIBLE_CHANGES = 30
