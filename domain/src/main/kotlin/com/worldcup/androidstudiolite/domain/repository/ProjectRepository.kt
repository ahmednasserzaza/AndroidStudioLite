package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.SearchMatch

interface ProjectRepository {
    suspend fun listProjects(): List<Project>
    suspend fun createProject(name: String, packageName: String, isPrivate: Boolean): Project
    suspend fun deleteProject(project: Project)

    suspend fun repairInfrastructure(project: Project)

    /** Creates the local folder + metadata for a repo about to be imported. */
    suspend fun createImportShell(repo: RemoteRepo): Project

    /** Fills in metadata (package name) detected from the pulled sources. */
    suspend fun finalizeImport(project: Project): Project
}

interface ProjectFilesRepository {
    suspend fun listFiles(project: Project): List<FileNode>
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, content: String)
    suspend fun createEntry(parentPath: String, name: String, isDirectory: Boolean): FileNode
    suspend fun rename(path: String, newName: String): String
    suspend fun delete(path: String)
    suspend fun searchFiles(project: Project, query: String): List<SearchMatch>
}
