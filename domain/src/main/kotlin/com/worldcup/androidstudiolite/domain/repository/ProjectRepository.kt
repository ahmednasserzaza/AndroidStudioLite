package com.worldcup.androidstudiolite.domain.repository

import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project

interface ProjectRepository {
    suspend fun listProjects(): List<Project>
    suspend fun createProject(name: String, packageName: String): Project
    suspend fun deleteProject(project: Project)

    suspend fun repairInfrastructure(project: Project)
}

interface ProjectFilesRepository {
    suspend fun listFiles(project: Project): List<FileNode>
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, content: String)
    suspend fun createEntry(parentPath: String, name: String, isDirectory: Boolean): FileNode
    suspend fun rename(path: String, newName: String): String
    suspend fun delete(path: String)
}
