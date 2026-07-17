package com.worldcup.androidstudiolite.feature.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslDangerButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.dialogs.AslDialog
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.tabs.AslTab
import com.worldcup.androidstudiolite.designsystem.components.tabs.AslTabRow
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.feature.base.CollectEffects
import com.worldcup.androidstudiolite.feature.editor.ui.CodeEditorField
import com.worldcup.androidstudiolite.feature.editor.ui.EditorSymbolBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToBuild: () -> Unit,
    onNavigateToProjects: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: EditorInteractionListener = viewModel

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            EditorEffect.NavigateToBuild -> onNavigateToBuild()
            EditorEffect.NavigateToProjects -> onNavigateToProjects()
        }
    }

    if (state.projectName == null) {
        NoProjectPlaceholder(onNavigateToProjects)
        return
    }

    val active = state.activeFile
    var fieldValue by remember(active?.path) {
        mutableStateOf(TextFieldValue(active?.content ?: ""))
    }
    val editorValue = if (active == null || fieldValue.text == active.content) {
        fieldValue
    } else {
        TextFieldValue(
            text = active.content,
            selection = TextRange(changedRegionEnd(fieldValue.text, active.content)),
        ).also { fieldValue = it }
    }

    fun insertSnippet(snippet: String) {
        val selection = editorValue.selection
        val newText = editorValue.text.replaceRange(selection.min, selection.max, snippet)
        fieldValue = TextFieldValue(newText, TextRange(selection.min + snippet.length))
        listener.onEditContent(newText)
    }

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AslTopBar(
                title = state.projectName ?: "",
                titleLeading = {
                    AslIcon(AslIcons.Android, tint = AslTheme.colors.primaryContainer)
                },
                actions = {
                    AslIconButton(
                        AslIcons.Folder,
                        onClick = listener::onToggleTree,
                        tint = if (state.treeVisible) AslTheme.colors.primary
                        else AslTheme.colors.onSurfaceVariant,
                        contentDescription = "Toggle file tree",
                    )
                    AslIconButton(
                        AslIcons.Save,
                        onClick = listener::onSaveAll,
                        contentDescription = "Save all",
                    )
                    AslIconButton(
                        AslIcons.Play,
                        onClick = listener::onRun,
                        tint = AslTheme.colors.primary,
                        contentDescription = "Run",
                    )
                },
                showDivider = false,
            )

            AslTabRow(
                tabs = state.openFiles.map {
                    AslTab(id = it.path, title = it.name, modified = it.dirty)
                },
                selectedId = state.activeFile?.path,
                onSelect = { listener.onSelectTab(it.id) },
                onClose = { listener.onCloseTab(it.id) },
            )

            Row(Modifier.weight(1f)) {
                if (state.treeVisible) {
                    FileTreePanel(
                        state = state,
                        listener = listener,
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight(),
                    )
                }
                if (active != null) {
                    CodeEditorField(
                        value = editorValue,
                        fileName = active.name,
                        onValueChange = { newValue ->
                            fieldValue = newValue
                            if (newValue.text != active.content) {
                                listener.onEditContent(newValue.text)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(AslTheme.colors.canvas),
                        contentAlignment = Alignment.Center,
                    ) {
                        AslText(
                            "Open a file from the tree",
                            color = AslTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }

            if (active != null && WindowInsets.isImeVisible) {
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                EditorSymbolBar(
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = listener::onUndo,
                    onRedo = listener::onRedo,
                    onInsert = ::insertSnippet,
                    onHideKeyboard = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
                )
            }
        }

        AslSnackbarHost(snackBar)

        state.fileAction?.let { target ->
            FileActionDialog(target, listener)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreePanel(
    state: EditorUiState,
    listener: EditorInteractionListener,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AslTheme.colors.panel),
        ) {
            items(state.visibleTree, key = { it.node.path }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(AslTheme.spacing.denseRowHeight)
                        .combinedClickable(
                            onClick = { listener.onNodeClick(row) },
                            onLongClick = { listener.onNodeLongPress(row) },
                        )
                        .padding(start = (8 + row.node.depth * 12).dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (row.node.isDirectory) {
                        AslIcon(
                            if (row.expanded) AslIcons.ExpandMore else AslIcons.ChevronRight,
                            size = 14.dp,
                            tint = AslTheme.colors.onSurfaceVariant,
                        )
                        AslIcon(
                            AslIcons.Folder,
                            size = 14.dp,
                            tint = AslTheme.colors.secondary,
                        )
                    } else {
                        AslIcon(
                            AslIcons.File,
                            size = 14.dp,
                            tint = AslTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 18.dp),
                        )
                    }
                    AslText(
                        row.node.name,
                        style = AslTheme.typography.uiLabelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(AslTheme.colors.divider),
        )
    }
}

@Composable
private fun FileActionDialog(target: FileActionTarget, listener: EditorInteractionListener) {
    var renameValue by remember { mutableStateOf(target.node.name) }
    var newEntryName by remember { mutableStateOf("") }

    AslDialog(
        title = target.node.name,
        onDismissRequest = listener::onDismissFileAction,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md)) {
            if (target.node.isDirectory) {
                AslTextField(
                    value = newEntryName,
                    onValueChange = { newEntryName = it },
                    label = "New file or folder inside ${target.node.name}",
                    placeholder = "NewFile.kt",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
                    AslGhostButton(
                        "New file",
                        onClick = {
                            listener.onCreateEntry(target.node.path, newEntryName, isDirectory = false)
                        },
                        enabled = newEntryName.isNotBlank(),
                    )
                    AslGhostButton(
                        "New folder",
                        onClick = {
                            listener.onCreateEntry(target.node.path, newEntryName, isDirectory = true)
                        },
                        enabled = newEntryName.isNotBlank(),
                    )
                }
                AslHorizontalDivider()
            }
            AslTextField(
                value = renameValue,
                onValueChange = { renameValue = it },
                label = "Rename",
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm, Alignment.End),
            ) {
                AslDangerButton(
                    "Delete",
                    onClick = { listener.onDeleteEntry(target.node.path) },
                )
                AslPrimaryButton(
                    "Rename",
                    onClick = { listener.onRenameEntry(target.node.path, renameValue) },
                    enabled = renameValue.isNotBlank() && renameValue != target.node.name,
                )
            }
        }
    }
}

private fun changedRegionEnd(old: String, new: String): Int {
    val minLen = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < minLen && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < minLen - prefix &&
        old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
    ) {
        suffix++
    }
    return new.length - suffix
}

@Composable
private fun NoProjectPlaceholder(onNavigateToProjects: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AslTheme.colors.background)
            .padding(AslTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AslIcon(AslIcons.Code, size = 40.dp, tint = AslTheme.colors.onSurfaceVariant)
        AslText("No project open", style = AslTheme.typography.title)
        AslText(
            "Open a project from the Project tab to start editing.",
            color = AslTheme.colors.onSurfaceVariant,
        )
        AslPrimaryButton("Go to Projects", onClick = onNavigateToProjects)
    }
}
