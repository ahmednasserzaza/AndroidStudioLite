package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslIndeterminateLinearProgress
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.feature.editor.EditorInteractionListener
import com.worldcup.androidstudiolite.feature.editor.EditorUiState

@Composable
fun EditorSearchBar(
    state: EditorUiState,
    listener: EditorInteractionListener,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(AslTheme.colors.panel)
            .padding(horizontal = AslTheme.spacing.sm, vertical = AslTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
        ) {
            AslTextField(
                value = state.searchQuery,
                onValueChange = listener::onSearchQueryChange,
                placeholder = "Search  (:42 = go to line)",
                modifier = Modifier.weight(1f),
                leading = {
                    AslIcon(
                        AslIcons.Search,
                        size = 14.dp,
                        tint = AslTheme.colors.onSurfaceVariant,
                    )
                },
            )
            if (!state.searchInProject) {
                AslText(
                    text = if (state.matches.isEmpty()) {
                        "0/0"
                    } else {
                        "${state.activeMatchIndex + 1}/${state.matches.size}"
                    },
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
                AslIconButton(
                    AslIcons.ExpandLess,
                    onClick = listener::onSearchPrev,
                    tint = AslTheme.colors.onSurfaceVariant,
                    contentDescription = "Previous match",
                )
                AslIconButton(
                    AslIcons.ExpandMore,
                    onClick = listener::onSearchNext,
                    tint = AslTheme.colors.onSurfaceVariant,
                    contentDescription = "Next match",
                )
            }
            AslIconButton(
                AslIcons.Close,
                onClick = listener::onToggleSearch,
                tint = AslTheme.colors.onSurfaceVariant,
                contentDescription = "Close search",
            )
        }

        state.goToLine?.let { line ->
            Box(
                Modifier
                    .background(AslTheme.colors.primary.copy(alpha = 0.14f), AslTheme.shapes.full)
                    .clickable { listener.onGoToLine() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                AslText(
                    "Go to line $line ↵",
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.primary,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
        ) {
            ScopeChip(
                text = "This file",
                selected = !state.searchInProject,
                onClick = { listener.onSearchScopeChange(false) },
            )
            ScopeChip(
                text = "Project",
                selected = state.searchInProject,
                onClick = { listener.onSearchScopeChange(true) },
            )
            Box(Modifier.weight(1f))
            if (!state.searchInProject) {
                AslTextField(
                    value = state.replaceQuery,
                    onValueChange = listener::onReplaceQueryChange,
                    placeholder = "Replace",
                    modifier = Modifier.weight(2f),
                )
                AslGhostButton(
                    "Replace",
                    onClick = listener::onReplaceCurrent,
                    enabled = state.activeMatchIndex >= 0,
                )
                AslGhostButton(
                    "All",
                    onClick = listener::onReplaceAll,
                    enabled = state.matches.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun ScopeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) AslTheme.colors.primary.copy(alpha = 0.14f) else AslTheme.colors.surfaceContainerHighest,
                AslTheme.shapes.full,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        AslText(
            text = text,
            style = AslTheme.typography.uiLabelSmall,
            color = if (selected) AslTheme.colors.primary else AslTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
fun ProjectSearchResults(
    state: EditorUiState,
    listener: EditorInteractionListener,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AslTheme.colors.canvas)) {
        if (state.searchingProject) {
            AslIndeterminateLinearProgress(Modifier.fillMaxWidth())
        }
        if (state.projectResults.isEmpty() && !state.searchingProject) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AslText(
                    if (state.searchQuery.isBlank()) "Type to search the whole project"
                    else "No results for \"${state.searchQuery}\"",
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.projectResults) { match ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { listener.onOpenSearchResult(match) }
                        .padding(
                            horizontal = AslTheme.spacing.md,
                            vertical = AslTheme.spacing.sm,
                        ),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs)) {
                        FileTypeBadge(match.fileName)
                        AslText(
                            match.relativePath,
                            style = AslTheme.typography.uiLabelSmall,
                            color = AslTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        AslText(
                            ":${match.lineNumber}",
                            style = AslTheme.typography.uiLabelSmall,
                            color = AslTheme.colors.onSurfaceVariant,
                        )
                    }
                    AslText(
                        match.lineText,
                        style = AslTheme.typography.codeSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AslHorizontalDivider()
            }
        }
    }
}
