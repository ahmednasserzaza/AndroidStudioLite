package com.worldcup.androidstudiolite.feature.editor

import com.worldcup.androidstudiolite.entities.BuildDiagnostic
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.SearchMatch
import com.worldcup.androidstudiolite.feature.editor.ui.LintIssue
import com.worldcup.androidstudiolite.session.OpenFileState
import com.worldcup.androidstudiolite.session.RecentFile

data class EditorUiState(
    val projectName: String? = null,
    val treeVisible: Boolean = false,
    val visibleTree: List<TreeRow> = emptyList(),
    val openFiles: List<OpenFileState> = emptyList(),
    val activeFile: OpenFileState? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val buildRunning: Boolean = false,
    val fileAction: FileActionTarget? = null,
    val searchVisible: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchInProject: Boolean = false,
    val matches: List<MatchRange> = emptyList(),
    val activeMatchIndex: Int = -1,
    val projectResults: List<SearchMatch> = emptyList(),
    val searchingProject: Boolean = false,
    val scrollRequest: ScrollRequest? = null,

    val diagnostics: List<BuildDiagnostic> = emptyList(),

    val lintIssues: List<LintIssue> = emptyList(),

    val changedLines: Set<Int> = emptySet(),

    val goToLine: Int? = null,
    val recentVisible: Boolean = false,
    val recentFiles: List<RecentFile> = emptyList(),
)

data class TreeRow(val node: FileNode, val expanded: Boolean)

data class FileActionTarget(val node: FileNode)

data class MatchRange(val start: Int, val end: Int)

data class ScrollRequest(val offset: Int, val nonce: Long)

sealed interface EditorEffect {
    data object NavigateToBuild : EditorEffect
    data object NavigateToProjects : EditorEffect
}

interface EditorInteractionListener {
    fun onToggleTree()
    fun onNodeClick(row: TreeRow)
    fun onNodeLongPress(row: TreeRow)
    fun onDismissFileAction()
    fun onCreateEntry(parentPath: String, name: String, isDirectory: Boolean)
    fun onRenameEntry(path: String, newName: String)
    fun onDeleteEntry(path: String)
    fun onSelectTab(path: String)
    fun onCloseTab(path: String)
    fun onEditContent(newText: String)
    fun onUndo()
    fun onRedo()
    fun onFlushSave()
    fun onRun()
    fun onToggleSearch()
    fun onSearchQueryChange(query: String)
    fun onReplaceQueryChange(text: String)
    fun onSearchNext()
    fun onSearchPrev()
    fun onReplaceCurrent()
    fun onReplaceAll()
    fun onSearchScopeChange(inProject: Boolean)
    fun onOpenSearchResult(match: SearchMatch)
    fun onGoToLine()
    fun onShowRecent(show: Boolean)
    fun onOpenRecent(path: String)
    fun onJumpToLine(line: Int)
}
