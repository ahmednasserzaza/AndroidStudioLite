package com.worldcup.androidstudiolite.feature.editor

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.files.CreateFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.DeleteFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.GetFileTreeUseCase
import com.worldcup.androidstudiolite.domain.files.ReadFileUseCase
import com.worldcup.androidstudiolite.domain.files.RenameFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.files.SearchProjectUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.entities.BuildDiagnostic
import com.worldcup.androidstudiolite.entities.BuildProgress
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.SearchMatch
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.feature.editor.ui.BalanceLint
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
    private val searchProject: SearchProjectUseCase,
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
    private var projectSearchJob: Job? = null
    private var lintJob: Job? = null
    private var lastLintedContent: String? = null

    private var buildDiagnostics: Map<String, List<BuildDiagnostic>> = emptyMap()

    private val editedSinceBuild = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(
                workspace.currentProject,
                workspace.openFiles,
                workspace.activeFilePath,
            ) { project, openFiles, activePath ->
                Triple(project, openFiles, activePath)
            }.collect { (project, openFiles, activePath) ->
                val active = openFiles.firstOrNull { f -> f.path == activePath }
                if (active != null) workspace.recordRecent(active)
                scheduleLint(active)
                updateState {
                    val matches = if (it.searchVisible && it.searchQuery.isNotEmpty() && active != null) {
                        computeMatches(active.content, it.searchQuery)
                    } else {
                        emptyList()
                    }
                    it.copy(
                        projectName = project?.name,
                        openFiles = openFiles,
                        activeFile = active,
                        matches = matches,
                        activeMatchIndex = if (matches.isEmpty()) {
                            -1
                        } else {
                            it.activeMatchIndex.coerceIn(0, matches.lastIndex)
                        },
                        canUndo = undoStacks[activePath]?.isNotEmpty() == true,
                        canRedo = redoStacks[activePath]?.isNotEmpty() == true,
                        diagnostics = diagnosticsFor(active),
                        changedLines = if (active != null) {
                            changedLines(active.originalContent, active.content)
                        } else {
                            emptySet()
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            buildSession.progress.collect { progress ->
                when (progress) {
                    is BuildProgress.Failed -> {
                        buildDiagnostics = progress.diagnostics.groupBy { it.relativePath }
                        editedSinceBuild.clear()
                    }
                    is BuildProgress.Success -> {
                        buildDiagnostics = emptyMap()
                        editedSinceBuild.clear()
                    }
                    else -> Unit
                }
                updateState {
                    it.copy(
                        buildRunning = progress is BuildProgress.Running,
                        diagnostics = diagnosticsFor(it.activeFile),
                    )
                }
            }
        }
        viewModelScope.launch {
            workspace.recentFiles.collect { recent ->
                updateState { it.copy(recentFiles = recent) }
            }
        }

        viewModelScope.launch {
            workspace.pendingOpen.collect { location ->
                val project = workspace.currentProject.value
                if (location != null && project != null) {
                    workspace.clearPendingOpen()
                    openFileAt("${project.path}/${location.relativePath}", location.line, location.column)
                }
            }
        }
        loadTree(revealMain = workspace.expandedDirs.isEmpty(), openMain = true)
    }

    private fun loadTree(revealMain: Boolean = false, openMain: Boolean = false) {
        val project = workspace.currentProject.value ?: return
        tryToExecute(
            callee = { getFileTree(project) },
            onSuccess = { nodes ->
                fullTree = nodes
                val main = nodes.firstOrNull { it.name == "MainActivity.kt" }

                if (revealMain && main != null) {
                    nodes.filter { it.isDirectory && main.path.startsWith(it.path + "/") }
                        .forEach { workspace.expandedDirs.add(it.path) }
                }
                rebuildVisibleTree()
                if (openMain && workspace.openFiles.value.isEmpty() && main != null) {
                    openFile(main)
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

    private fun openFileAt(path: String, line: Int, column: Int) {
        val existing = workspace.openFiles.value.firstOrNull { it.path == path }
        if (existing != null) {
            workspace.setActiveFile(path)
            requestScrollTo(existing.content, line, column)
            return
        }
        tryToExecute(
            callee = { readFile(path) },
            onSuccess = { content ->
                val opened = OpenFileState(
                    path = path,
                    relativePath = relativeTo(path),
                    name = path.substringAfterLast('/'),
                    content = content,
                    dirty = false,
                )
                workspace.setOpenFiles(workspace.openFiles.value + opened)
                workspace.setActiveFile(path)
                requestScrollTo(content, line, column)
            },
            onError = { showSnackBar("Can't open ${path.substringAfterLast('/')}") },
        )
    }

    private fun requestScrollTo(content: String, line: Int, column: Int) {
        val offset = offsetOfLocation(content, line, column)
        updateState { it.copy(scrollRequest = ScrollRequest(offset, System.nanoTime())) }
    }

    private fun offsetOfLocation(content: String, line: Int, column: Int): Int {
        var offset = 0
        var current = 1
        while (current < line) {
            val next = content.indexOf('\n', offset)
            if (next == -1) break
            offset = next + 1
            current++
        }
        val lineEnd = content.indexOf('\n', offset).let { if (it == -1) content.length else it }
        return (offset + (column - 1).coerceAtLeast(0)).coerceAtMost(lineEnd)
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
        markEditedSinceBuild(current)
        setActiveContent(activePath, newText)
        scheduleAutoSave()
    }

    private fun markEditedSinceBuild(file: OpenFileState) {
        if (file.relativePath in editedSinceBuild) return
        editedSinceBuild.add(file.relativePath)
        updateState { it.copy(diagnostics = diagnosticsFor(it.activeFile)) }
    }

    private fun diagnosticsFor(file: OpenFileState?): List<BuildDiagnostic> {
        file ?: return emptyList()
        if (file.relativePath in editedSinceBuild) return emptyList()
        return buildDiagnostics[file.relativePath].orEmpty()
    }

    private fun scheduleLint(active: OpenFileState?) {
        if (active == null) {
            lastLintedContent = null
            updateState { it.copy(lintIssues = emptyList()) }
            return
        }
        if (active.content == lastLintedContent) return
        lintJob?.cancel()
        lintJob = viewModelScope.launch {
            delay(LINT_DEBOUNCE_MS)
            lastLintedContent = active.content
            val issues = BalanceLint.check(active.content, active.name)
            updateState { it.copy(lintIssues = issues) }
        }
    }

    private fun changedLines(original: String, current: String): Set<Int> {
        if (original == current) return emptySet()
        if (current.length > MAX_SEARCHABLE_CHARS) return emptySet()
        val originalLines = original.lines()
        val currentLines = current.lines()
        var prefix = 0
        while (prefix < originalLines.size && prefix < currentLines.size &&
            originalLines[prefix] == currentLines[prefix]
        ) {
            prefix++
        }
        var suffix = 0
        while (suffix < originalLines.size - prefix && suffix < currentLines.size - prefix &&
            originalLines[originalLines.size - 1 - suffix] == currentLines[currentLines.size - 1 - suffix]
        ) {
            suffix++
        }
        return (prefix until (currentLines.size - suffix)).toSet()
    }

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

    override fun onToggleSearch() {
        updateState {
            if (it.searchVisible) {
                it.copy(
                    searchVisible = false,
                    searchQuery = "",
                    replaceQuery = "",
                    searchInProject = false,
                    matches = emptyList(),
                    activeMatchIndex = -1,
                    projectResults = emptyList(),
                    searchingProject = false,
                )
            } else {
                it.copy(searchVisible = true)
            }
        }
    }

    override fun onSearchQueryChange(query: String) {
        val goToLine = GO_TO_LINE_REGEX.find(query)?.groupValues?.get(1)?.toIntOrNull()
        val active = currentState().activeFile
        val matches = if (active != null && goToLine == null) {
            computeMatches(active.content, query)
        } else {
            emptyList()
        }
        updateState {
            it.copy(
                searchQuery = query,
                matches = matches,
                activeMatchIndex = if (matches.isEmpty()) -1 else 0,
                goToLine = goToLine,
            )
        }
        if (matches.isNotEmpty()) scrollToMatch(0)
        if (currentState().searchInProject) scheduleProjectSearch()
    }

    override fun onGoToLine() {
        val line = currentState().goToLine ?: return
        val active = currentState().activeFile ?: return
        requestScrollTo(active.content, line, 1)
    }

    override fun onJumpToLine(line: Int) {
        val active = currentState().activeFile ?: return
        requestScrollTo(active.content, line, 1)
    }

    override fun onShowRecent(show: Boolean) {
        updateState { it.copy(recentVisible = show) }
    }

    override fun onOpenRecent(path: String) {
        updateState { it.copy(recentVisible = false) }
        val existing = workspace.openFiles.value.firstOrNull { it.path == path }
        if (existing != null) {
            onFlushSave()
            workspace.setActiveFile(path)
        } else {
            openFileAt(path, 1, 1)
        }
    }

    override fun onReplaceQueryChange(text: String) {
        updateState { it.copy(replaceQuery = text) }
    }

    override fun onSearchNext() = stepMatch(+1)

    override fun onSearchPrev() = stepMatch(-1)

    private fun stepMatch(delta: Int) {
        val state = currentState()
        if (state.matches.isEmpty()) return
        val next = (state.activeMatchIndex + delta).mod(state.matches.size)
        updateState { it.copy(activeMatchIndex = next) }
        scrollToMatch(next)
    }

    private fun scrollToMatch(index: Int) {
        val match = currentState().matches.getOrNull(index) ?: return
        updateState { it.copy(scrollRequest = ScrollRequest(match.start, System.nanoTime())) }
    }

    override fun onReplaceCurrent() {
        val state = currentState()
        val active = state.activeFile ?: return
        val match = state.matches.getOrNull(state.activeMatchIndex) ?: return
        val newText = active.content.replaceRange(match.start, match.end, state.replaceQuery)
        applyProgrammaticEdit(active, newText)
        updateState { it.copy(scrollRequest = ScrollRequest(match.start, System.nanoTime())) }
    }

    override fun onReplaceAll() {
        val state = currentState()
        val active = state.activeFile ?: return
        if (state.matches.isEmpty()) return
        val builder = StringBuilder(active.content)
        for (match in state.matches.asReversed()) {
            builder.replace(match.start, match.end, state.replaceQuery)
        }
        applyProgrammaticEdit(active, builder.toString())
        showSnackBar("Replaced ${state.matches.size} occurrence(s)")
    }

    private fun applyProgrammaticEdit(current: OpenFileState, newText: String) {
        if (current.content == newText) return
        val stack = undoStacks.getOrPut(current.path) { ArrayDeque() }
        stack.addLast(current.content)
        if (stack.size > UNDO_LIMIT) stack.removeFirst()
        redoStacks[current.path]?.clear()
        lastEditPath = null
        markEditedSinceBuild(current)
        setActiveContent(current.path, newText)
        scheduleAutoSave()
    }

    override fun onSearchScopeChange(inProject: Boolean) {
        updateState { it.copy(searchInProject = inProject) }
        if (inProject) scheduleProjectSearch(immediate = true)
    }

    private fun scheduleProjectSearch(immediate: Boolean = false) {
        projectSearchJob?.cancel()
        val project = workspace.currentProject.value ?: return
        val query = currentState().searchQuery
        if (query.isBlank()) {
            updateState { it.copy(projectResults = emptyList(), searchingProject = false) }
            return
        }
        projectSearchJob = viewModelScope.launch {
            if (!immediate) delay(PROJECT_SEARCH_DEBOUNCE_MS)
            updateState { it.copy(searchingProject = true) }
            tryToExecute(
                callee = { searchProject(project, query) },
                onSuccess = { results ->
                    updateState { it.copy(projectResults = results, searchingProject = false) }
                },
                onError = { updateState { it.copy(searchingProject = false) } },
                inScope = this,
            ).join()
        }
    }

    override fun onOpenSearchResult(match: SearchMatch) {
        updateState { it.copy(searchInProject = false) }
        openFileAt(match.path, match.lineNumber, match.columnStart + 1)
    }

    private fun computeMatches(content: String, query: String): List<MatchRange> {
        if (query.isEmpty() || content.length > MAX_SEARCHABLE_CHARS) return emptyList()
        val matches = mutableListOf<MatchRange>()
        var index = content.indexOf(query, 0, ignoreCase = true)
        while (index >= 0 && matches.size < MAX_MATCHES) {
            matches += MatchRange(index, index + query.length)
            index = content.indexOf(query, index + query.length, ignoreCase = true)
        }
        return matches
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
        const val MAX_MATCHES = 500
        const val MAX_SEARCHABLE_CHARS = 200_000
        const val PROJECT_SEARCH_DEBOUNCE_MS = 350L
        const val LINT_DEBOUNCE_MS = 450L
        val GO_TO_LINE_REGEX = Regex("^:(\\d+)$")
    }
}
