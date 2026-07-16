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
        workspace.setOpenFiles(remaining)
        if (workspace.activeFilePath.value?.startsWith(path) == true) {
            workspace.setActiveFile(remaining.firstOrNull()?.path)
        }
    }

    override fun onSelectTab(path: String) {
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
        workspace.setOpenFiles(remaining)
        if (workspace.activeFilePath.value == path) {
            workspace.setActiveFile(
                remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.path,
            )
        }
    }

    override fun onEditContent(newText: String) {
        val activePath = workspace.activeFilePath.value ?: return
        workspace.setOpenFiles(
            workspace.openFiles.value.map {
                if (it.path == activePath) it.copy(content = newText, dirty = true) else it
            },
        )
    }

    override fun onSaveAll() {
        val dirty = workspace.openFiles.value.filter { it.dirty }
        if (dirty.isEmpty()) return
        tryToExecute(
            callee = { dirty.forEach { saveFile(it.path, it.content) } },
            onSuccess = {
                workspace.setOpenFiles(
                    workspace.openFiles.value.map { it.copy(dirty = false) },
                )
                showSnackBar("Saved ${dirty.size} file${if (dirty.size > 1) "s" else ""}")
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
}
