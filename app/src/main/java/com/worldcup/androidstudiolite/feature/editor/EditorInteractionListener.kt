package com.worldcup.androidstudiolite.feature.editor

import com.worldcup.androidstudiolite.entities.SearchMatch

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
