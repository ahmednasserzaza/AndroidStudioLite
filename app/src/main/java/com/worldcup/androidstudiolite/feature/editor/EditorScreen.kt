package com.worldcup.androidstudiolite.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.worldcup.androidstudiolite.feature.editor.ui.EditorScrollRequest
import com.worldcup.androidstudiolite.feature.editor.ui.EditorSearchBar
import com.worldcup.androidstudiolite.feature.editor.ui.EditorSymbolBar
import com.worldcup.androidstudiolite.feature.editor.ui.MatchHighlight
import com.worldcup.androidstudiolite.feature.editor.ui.ProjectSearchResults

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

    // Save-on-navigate: flush pending edits when the app is backgrounded.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { listener.onFlushSave() }

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

    fun toggleComment() {
        val current = active ?: return
        val updated = toggleLineComment(editorValue, current.name)
        fieldValue = updated
        listener.onEditContent(updated.text)
    }

    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= 600

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AslTopBar(
                title = state.projectName ?: "",
                titleLeading = {
                    AslIcon(AslIcons.Android, tint = AslTheme.colors.primaryContainer)
                },
                actions = {
                    AslIconButton(
                        AslIcons.Search,
                        onClick = listener::onToggleSearch,
                        tint = if (state.searchVisible) AslTheme.colors.primary
                        else AslTheme.colors.onSurfaceVariant,
                        contentDescription = "Search",
                    )
                    AslIconButton(
                        AslIcons.Folder,
                        onClick = listener::onToggleTree,
                        tint = if (state.treeVisible) AslTheme.colors.primary
                        else AslTheme.colors.onSurfaceVariant,
                        contentDescription = "Toggle file tree",
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

            if (state.searchVisible) {
                EditorSearchBar(state = state, listener = listener)
            }

            // Place the cursor at the target of a jump (search match, diagnostic).
            LaunchedEffect(state.scrollRequest) {
                val request = state.scrollRequest ?: return@LaunchedEffect
                if (request.offset <= editorValue.text.length) {
                    fieldValue = editorValue.copy(selection = TextRange(request.offset))
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize()) {
                    if (wide && state.treeVisible) {
                        FileTreePanel(
                            state = state,
                            listener = listener,
                            modifier = Modifier.width(240.dp).fillMaxHeight(),
                        )
                    }
                    if (state.searchVisible && state.searchInProject) {
                        ProjectSearchResults(
                            state = state,
                            listener = listener,
                            modifier = Modifier.weight(1f),
                        )
                    } else if (active != null) {
                        CodeEditorField(
                            value = editorValue,
                            fileName = active.name,
                            onValueChange = { newValue ->
                                val adjusted = autoIndentNewline(editorValue, newValue)
                                fieldValue = adjusted
                                if (adjusted.text != active.content) {
                                    listener.onEditContent(adjusted.text)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            searchMatches = state.matches.mapIndexed { index, match ->
                                MatchHighlight(
                                    start = match.start,
                                    end = match.end,
                                    active = index == state.activeMatchIndex,
                                )
                            },
                            scrollRequest = state.scrollRequest?.let {
                                EditorScrollRequest(it.offset, it.nonce)
                            },
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

                // Overlay drawer on narrow (phone) windows.
                if (!wide) {
                    FileTreeDrawer(state = state, listener = listener)
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
                    onToggleComment = ::toggleComment,
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

@Composable
private fun FileTreeDrawer(
    state: EditorUiState,
    listener: EditorInteractionListener,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.treeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = listener::onToggleTree,
                    ),
            )
        }
        AnimatedVisibility(
            visible = state.treeVisible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            FileTreePanel(
                state = state,
                listener = listener,
                modifier = Modifier.width(280.dp).fillMaxHeight(),
            )
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
        Box(
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

/**
 * When a single newline is typed, carry the previous line's leading whitespace onto
 * the new line, plus one indent step after an opening `{` or `(`.
 */
private fun autoIndentNewline(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (new.text.length != old.text.length + 1) return new
    if (!new.selection.collapsed) return new
    val insertPos = new.selection.start - 1
    if (insertPos < 0 || insertPos >= new.text.length || new.text[insertPos] != '\n') return new

    val lineStart = new.text.lastIndexOf('\n', insertPos - 1) + 1
    val prevLine = new.text.substring(lineStart, insertPos)
    val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
    val trimmed = prevLine.trimEnd()
    val extra = if (trimmed.endsWith("{") || trimmed.endsWith("(")) INDENT_STEP else ""
    val insertion = indent + extra
    if (insertion.isEmpty()) return new

    val newText = new.text.substring(0, insertPos + 1) + insertion + new.text.substring(insertPos + 1)
    return TextFieldValue(newText, TextRange(insertPos + 1 + insertion.length))
}

/** Comments or uncomments every line touched by the current selection. */
private fun toggleLineComment(value: TextFieldValue, fileName: String): TextFieldValue {
    val token = lineCommentToken(fileName)
    val text = value.text
    val selMin = value.selection.min
    val selMax = value.selection.max

    val blockStart = text.lastIndexOf('\n', selMin - 1) + 1
    val blockEnd = text.indexOf('\n', selMax).let { if (it == -1) text.length else it }
    val block = text.substring(blockStart, blockEnd)

    val lines = block.split('\n')
    val nonBlank = lines.filter { it.isNotBlank() }
    val allCommented = nonBlank.isNotEmpty() && nonBlank.all { it.trimStart().startsWith(token) }

    val newLines = lines.map { line ->
        if (line.isBlank()) return@map line
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val rest = line.substring(indent.length)
        if (allCommented) {
            var stripped = rest.removePrefix(token)
            if (stripped.startsWith(" ")) stripped = stripped.substring(1)
            indent + stripped
        } else {
            "$indent$token $rest"
        }
    }
    val newBlock = newLines.joinToString("\n")
    val newText = text.substring(0, blockStart) + newBlock + text.substring(blockEnd)
    return TextFieldValue(newText, TextRange(blockStart, blockStart + newBlock.length))
}

private fun lineCommentToken(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "py", "sh", "rb", "yml", "yaml", "toml", "properties", "cfg" -> "#"
        else -> "//"
    }

private const val INDENT_STEP = "    "

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
