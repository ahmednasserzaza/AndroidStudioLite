package com.worldcup.androidstudiolite.feature.editor

import com.worldcup.androidstudiolite.entities.BuildDiagnostic
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.SearchMatch
import com.worldcup.androidstudiolite.feature.editor.ui.LintIssue
import com.worldcup.androidstudiolite.session.OpenFileState
import com.worldcup.androidstudiolite.session.RecentFile

data class EditorScreenState(
    val projectName: String? = null,
    val branch: String = "",
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
