package com.worldcup.androidstudiolite.feature.editor

import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.session.OpenFileState

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
)

data class TreeRow(val node: FileNode, val expanded: Boolean)

data class FileActionTarget(val node: FileNode)

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
}
