package com.worldcup.androidstudiolite.session

import com.worldcup.androidstudiolite.entities.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceSession {

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject = _currentProject.asStateFlow()

    private val _openFiles = MutableStateFlow<List<OpenFileState>>(emptyList())
    val openFiles = _openFiles.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String?>(null)
    val activeFilePath = _activeFilePath.asStateFlow()

    private val _pendingOpen = MutableStateFlow<OpenLocation?>(null)
    val pendingOpen = _pendingOpen.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    val recentFiles = _recentFiles.asStateFlow()

    val expandedDirs = mutableSetOf<String>()

    fun recordRecent(file: OpenFileState) {
        val entry = RecentFile(file.path, file.name, file.relativePath)
        _recentFiles.value =
            (listOf(entry) + _recentFiles.value.filterNot { it.path == entry.path })
                .take(RECENT_LIMIT)
    }

    fun requestOpen(location: OpenLocation) {
        _pendingOpen.value = location.copy(nonce = System.nanoTime())
    }

    fun clearPendingOpen() {
        _pendingOpen.value = null
    }

    fun openProject(project: Project) {
        _currentProject.value = project
        _openFiles.value = emptyList()
        _activeFilePath.value = null
        _pendingOpen.value = null
        _recentFiles.value = emptyList()
        expandedDirs.clear()
    }

    fun closeProject() {
        _currentProject.value = null
        _openFiles.value = emptyList()
        _activeFilePath.value = null
        _pendingOpen.value = null
        _recentFiles.value = emptyList()
        expandedDirs.clear()
    }

    fun setOpenFiles(files: List<OpenFileState>) {
        _openFiles.value = files
    }

    fun setActiveFile(path: String?) {
        _activeFilePath.value = path
    }
}

data class OpenFileState(
    val path: String,
    val relativePath: String,
    val name: String,
    val content: String,
    val dirty: Boolean,

    val originalContent: String = content,
)

data class OpenLocation(
    val relativePath: String,
    val line: Int,
    val column: Int = 1,
    val nonce: Long = 0L,
)

data class RecentFile(
    val path: String,
    val name: String,
    val relativePath: String,
)

private const val RECENT_LIMIT = 20
