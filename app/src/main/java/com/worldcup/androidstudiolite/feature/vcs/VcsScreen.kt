package com.worldcup.androidstudiolite.feature.vcs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.feature.base.CollectEffects

@Composable
fun VcsScreen(
    viewModel: VcsViewModel,
    onNavigateToProjects: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: VcsInteractionListener = viewModel

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            VcsEffect.NavigateToProjects -> onNavigateToProjects()
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
                title = "Version Control",
                titleLeading = {
                    AslIcon(AslIcons.GitBranch, tint = AslTheme.colors.primaryContainer)
                },
                actions = {
                    AslIconButton(
                        AslIcons.Refresh,
                        onClick = listener::onLoadCommits,
                        contentDescription = "Refresh",
                    )
                },
            )

            Column(
                Modifier.padding(AslTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                AslCard(Modifier.fillMaxWidth(), contentPadding = AslTheme.spacing.md) {
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

                AslText("History", style = AslTheme.typography.uiHeader)
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = AslTheme.spacing.gutter,
                ),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
            ) {
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
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                    }
                }
            }
        }

        AslSnackbarHost(snackBar)
    }
}
