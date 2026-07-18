package com.worldcup.androidstudiolite.feature.editor

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.files.CreateFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.DeleteFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.GetFileTreeUseCase
import com.worldcup.androidstudiolite.domain.files.ReadFileUseCase
import com.worldcup.androidstudiolite.domain.files.RenameFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.BuildSession
import com.worldcup.androidstudiolite.session.OpenFileState
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EditorViewModel(
    private val getFileTree: GetFileTreeUseCase,
    private val readFile: ReadFileUseCase,
    private val saveFile: SaveFileUseCase,
    private val createEntry: CreateFileEntryUseCase,
    private val renameEntry: RenameFileEntryUseCase,
    private val deleteEntry: DeleteFileEntryUseCase,
    private val repairInfrastructure: RepairProjectInfrastructureUseCase,
    private val workspace: WorkspaceSession,
    private val buildSession: BuildSession,
) : BaseViewModel<EditorUiState, EditorEffect>(EditorUiState()),
    EditorInteractionListener {

    private var fullTree: List<FileNode> = emptyList()

    private val undoStacks = mutableMapOf<String, ArrayDeque<String>>()
    private val redoStacks = mutableMapOf<String, ArrayDeque<String>>()
    private var lastEditPath: String? = null
    private var lastEditAtMs = 0L
    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                workspace.currentProject,
                workspace.openFiles,
                workspace.activeFilePath,
            ) { project, openFiles, activePath ->
                Triple(project, openFiles, activePath)
            }.collect { (project, openFiles, activePath) ->
                updateState {
                    it.copy(
                        projectName = project?.name,
                        openFiles = openFiles,
                        activeFile = openFiles.firstOrNull { f -> f.path == activePath },
                        canUndo = undoStacks[activePath]?.isNotEmpty() == true,
                        canRedo = redoStacks[activePath]?.isNotEmpty() == true,
                    )
                }
            }
        }
        viewModelScope.launch {
            buildSession.progress.collect { progress ->
                updateState {
                    it.copy(
                        buildRunning = progress is com.worldcup.androidstudiolite.entities.BuildProgress.Running,
                    )
                }
            }
        }
        loadTree(expandAll = workspace.expandedDirs.isEmpty(), openMain = true)
    }

    private fun loadTree(expandAll: Boolean = false, openMain: Boolean = false) {
        val project = workspace.currentProject.value ?: return
        tryToExecute(
            callee = { getFileTree(project) },
            onSuccess = { nodes ->
                fullTree = nodes
                if (expandAll) {
                    workspace.expandedDirs.addAll(nodes.filter { it.isDirectory }.map { it.path })
                }
                rebuildVisibleTree()
                if (openMain && workspace.openFiles.value.isEmpty()) {
                    nodes.firstOrNull { it.name == "MainActivity.kt" }?.let { openFile(it) }
                }
            },
        )
    }

    private fun rebuildVisibleTree() {
        val visible = mutableListOf<TreeRow>()
        var skipDeeperThan: Int? = null
        for (node in fullTree) {
            val skipping = skipDeeperThan
            if (skipping != null) {
                if (node.depth > skipping) continue
                skipDeeperThan = null
            }
            val expanded = node.path in workspace.expandedDirs
            visible += TreeRow(node, expanded)
            if (node.isDirectory && !expanded) skipDeeperThan = node.depth
        }
        updateState { it.copy(visibleTree = visible) }
    }

    override fun onToggleTree() {
        updateState { it.copy(treeVisible = !it.treeVisible) }
    }

    override fun onNodeClick(row: TreeRow) {
        if (row.node.isDirectory) {
            if (!workspace.expandedDirs.remove(row.node.path)) {
                workspace.expandedDirs.add(row.node.path)
            }
            rebuildVisibleTree()
        } else {
            openFile(row.node)
            // Auto-close the drawer so opening a file is a single gesture.
            updateState { it.copy(treeVisible = false) }
        }
    }

    private fun openFile(node: FileNode) {
        val existing = workspace.openFiles.value.firstOrNull { it.path == node.path }
        if (existing != null) {
            workspace.setActiveFile(existing.path)
            return
        }
        tryToExecute(
            callee = { readFile(node.path) },
            onSuccess = { content ->
                val opened = OpenFileState(
                    path = node.path,
                    relativePath = node.relativePath,
                    name = node.name,
                    content = content,
                    dirty = false,
                )
                workspace.setOpenFiles(workspace.openFiles.value + opened)
                workspace.setActiveFile(opened.path)
            },
            onError = { showSnackBar("Can't open ${node.name}: not a text file") },
        )
    }

    override fun onNodeLongPress(row: TreeRow) {
        updateState { it.copy(fileAction = FileActionTarget(row.node)) }
    }

    override fun onDismissFileAction() {
        updateState { it.copy(fileAction = null) }
    }

    override fun onCreateEntry(parentPath: String, name: String, isDirectory: Boolean) {
        tryToExecute(
            callee = { createEntry(parentPath, name, isDirectory) },
            onSuccess = { node ->
                workspace.expandedDirs.add(parentPath)
                updateState { it.copy(fileAction = null) }
                loadTree()
                if (!isDirectory) {
                    openFile(node.copy(relativePath = relativeTo(node.path)))
                }
            },
        )
    }

    override fun onRenameEntry(path: String, newName: String) {
        closeTabsUnder(path)
        tryToExecute(
            callee = { renameEntry(path, newName) },
            onSuccess = {
                updateState { it.copy(fileAction = null) }
                loadTree()
            },
        )
    }

    override fun onDeleteEntry(path: String) {
        closeTabsUnder(path)
        tryToExecute(
            callee = { deleteEntry(path) },
            onSuccess = {
                updateState { it.copy(fileAction = null) }
                loadTree()
            },
        )
    }

    private fun closeTabsUnder(path: String) {
        val remaining = workspace.openFiles.value.filterNot { it.path.startsWith(path) }
        undoStacks.keys.removeAll { it.startsWith(path) }
        redoStacks.keys.removeAll { it.startsWith(path) }
        workspace.setOpenFiles(remaining)
        if (workspace.activeFilePath.value?.startsWith(path) == true) {
            workspace.setActiveFile(remaining.firstOrNull()?.path)
        }
    }

    override fun onSelectTab(path: String) {
        if (path == workspace.activeFilePath.value) return
        onFlushSave()
        workspace.setActiveFile(path)
    }

    override fun onCloseTab(path: String) {
        val files = workspace.openFiles.value
        val closing = files.firstOrNull { it.path == path } ?: return
        if (closing.dirty) {
            tryToExecute(callee = { saveFile(closing.path, closing.content) })
        }
        val index = files.indexOf(closing)
        val remaining = files - closing
        undoStacks.remove(path)
        redoStacks.remove(path)
        workspace.setOpenFiles(remaining)
        if (workspace.activeFilePath.value == path) {
            workspace.setActiveFile(
                remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.path,
            )
        }
    }

    override fun onEditContent(newText: String) {
        val activePath = workspace.activeFilePath.value ?: return
        val current = workspace.openFiles.value.firstOrNull { it.path == activePath } ?: return
        if (current.content == newText) return
        val now = System.currentTimeMillis()
        if (activePath != lastEditPath || now - lastEditAtMs > UNDO_COALESCE_MS) {
            val stack = undoStacks.getOrPut(activePath) { ArrayDeque() }
            stack.addLast(current.content)
            if (stack.size > UNDO_LIMIT) stack.removeFirst()
            redoStacks[activePath]?.clear()
        }
        lastEditPath = activePath
        lastEditAtMs = now
        setActiveContent(activePath, newText)
        scheduleAutoSave()
    }

    /**
     * Debounced persistence of dirty buffers. Restarted on every keystroke so the
     * write only fires once typing settles. Does not touch the undo/redo stacks.
     */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            persistDirtyFiles()
        }
    }

    private fun persistDirtyFiles() {
        val dirty = workspace.openFiles.value.filter { it.dirty }
        if (dirty.isEmpty()) return
        tryToExecute(
            callee = { dirty.forEach { saveFile(it.path, it.content) } },
            onSuccess = {
                // Only clear the dirty flag for files whose content is unchanged
                // since the write started, so edits made mid-save stay dirty.
                workspace.setOpenFiles(
                    workspace.openFiles.value.map { file ->
                        val saved = dirty.firstOrNull { it.path == file.path }
                        if (saved != null && saved.content == file.content) {
                            file.copy(dirty = false)
                        } else {
                            file
                        }
                    },
                )
            },
        )
    }

    /**
     * Flush pending edits immediately. Called on tab switch (save-on-navigate) and
     * when the app is backgrounded (lifecycle ON_STOP/ON_PAUSE) so nothing is lost.
     */
    override fun onFlushSave() {
        autoSaveJob?.cancel()
        persistDirtyFiles()
    }

    override fun onUndo() {
        val activePath = workspace.activeFilePath.value ?: return
        val current = workspace.openFiles.value.firstOrNull { it.path == activePath } ?: return
        val previous = undoStacks[activePath]?.removeLastOrNull() ?: return
        redoStacks.getOrPut(activePath) { ArrayDeque() }.addLast(current.content)
        lastEditPath = null
        setActiveContent(activePath, previous)
    }

    override fun onRedo() {
        val activePath = workspace.activeFilePath.value ?: return
        val current = workspace.openFiles.value.firstOrNull { it.path == activePath } ?: return
        val next = redoStacks[activePath]?.removeLastOrNull() ?: return
        undoStacks.getOrPut(activePath) { ArrayDeque() }.addLast(current.content)
        lastEditPath = null
        setActiveContent(activePath, next)
    }

    private fun setActiveContent(path: String, newText: String) {
        workspace.setOpenFiles(
            workspace.openFiles.value.map {
                if (it.path == path) it.copy(content = newText, dirty = true) else it
            },
        )
    }

    override fun onRun() {
        val project = workspace.currentProject.value ?: return
        if (buildSession.isRunning) {
            sendNewEffect(EditorEffect.NavigateToBuild)
            return
        }
        tryToExecute(
            callee = {
                workspace.openFiles.value.filter { it.dirty }.forEach {
                    saveFile(it.path, it.content)
                }
                workspace.setOpenFiles(workspace.openFiles.value.map { it.copy(dirty = false) })
                repairInfrastructure(project)
            },
            onSuccess = {
                buildSession.start(project)
                sendNewEffect(EditorEffect.NavigateToBuild)
            },
        )
    }

    private fun relativeTo(path: String): String {
        val root = workspace.currentProject.value?.path ?: return path
        return path.removePrefix("$root/")
    }

    private companion object {
        const val UNDO_COALESCE_MS = 800L
        const val UNDO_LIMIT = 100
        const val AUTO_SAVE_DEBOUNCE_MS = 1200L
    }
}
