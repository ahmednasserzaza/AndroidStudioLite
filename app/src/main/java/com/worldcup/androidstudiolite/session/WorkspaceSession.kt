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

    val expandedDirs = mutableSetOf<String>()

    fun openProject(project: Project) {
        _currentProject.value = project
        _openFiles.value = emptyList()
        _activeFilePath.value = null
        expandedDirs.clear()
    }

    fun closeProject() {
        _currentProject.value = null
        _openFiles.value = emptyList()
        _activeFilePath.value = null
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
)
