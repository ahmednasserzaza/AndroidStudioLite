package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.local.fs.ProjectFileSystemDataSource
import com.worldcup.androidstudiolite.domain.repository.ProjectFilesRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val fs: ProjectFileSystemDataSource,
) : ProjectRepository {

    override suspend fun listProjects(): List<Project> = withContext(Dispatchers.IO) {
        fs.listProjects()
    }

    override suspend fun createProject(name: String, packageName: String): Project =
        withContext(Dispatchers.IO) { fs.createProject(name, packageName) }

    override suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        fs.deleteProject(project)
    }

    override suspend fun repairInfrastructure(project: Project) = withContext(Dispatchers.IO) {
        fs.repairInfrastructure(project)
    }
}

class ProjectFilesRepositoryImpl(
    private val fs: ProjectFileSystemDataSource,
) : ProjectFilesRepository {

    override suspend fun listFiles(project: Project): List<FileNode> =
        withContext(Dispatchers.IO) { fs.listFiles(project) }

    override suspend fun readFile(path: String): String =
        withContext(Dispatchers.IO) { fs.readFile(path) }

    override suspend fun writeFile(path: String, content: String) =
        withContext(Dispatchers.IO) { fs.writeFile(path, content) }

    override suspend fun createEntry(parentPath: String, name: String, isDirectory: Boolean): FileNode =
        withContext(Dispatchers.IO) { fs.createEntry(parentPath, name, isDirectory) }

    override suspend fun rename(path: String, newName: String): String =
        withContext(Dispatchers.IO) { fs.rename(path, newName) }

    override suspend fun delete(path: String) =
        withContext(Dispatchers.IO) { fs.delete(path) }
}
