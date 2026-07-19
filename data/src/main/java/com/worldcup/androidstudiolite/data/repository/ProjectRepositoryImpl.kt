package com.worldcup.androidstudiolite.data.repository

import com.worldcup.androidstudiolite.data.local.fs.ProjectFileSystemDataSource
import com.worldcup.androidstudiolite.domain.repository.ProjectFilesRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.SearchMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val fs: ProjectFileSystemDataSource,
) : ProjectRepository {

    override suspend fun listProjects(): List<Project> = withContext(Dispatchers.IO) {
        fs.listProjects()
    }

    override suspend fun createProject(name: String, packageName: String, isPrivate: Boolean): Project =
        withContext(Dispatchers.IO) { fs.createProject(name, packageName, isPrivate) }

    override suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        fs.deleteProject(project)
    }

    override suspend fun repairInfrastructure(project: Project) = withContext(Dispatchers.IO) {
        fs.repairInfrastructure(project)
    }

    override suspend fun createImportShell(repo: RemoteRepo): Project =
        withContext(Dispatchers.IO) { fs.createImportShell(repo) }

    override suspend fun finalizeImport(project: Project): Project =
        withContext(Dispatchers.IO) { fs.finalizeImport(project) }

    override suspend fun setBranch(project: Project, branch: String): Project =
        withContext(Dispatchers.IO) { fs.setBranch(project, branch) }

    override suspend fun recordSynced(project: Project) =
        withContext(Dispatchers.IO) { fs.recordSynced(project) }

    override suspend fun localChanges(project: Project): List<FileChange> =
        withContext(Dispatchers.IO) { fs.localChanges(project) }

    override suspend fun clearWorkingTree(project: Project) =
        withContext(Dispatchers.IO) { fs.clearWorkingTree(project) }

    override suspend fun restoreFile(project: Project, relativePath: String, bytes: ByteArray) =
        withContext(Dispatchers.IO) { fs.restoreFile(project, relativePath, bytes) }

    override suspend fun deleteFile(project: Project, relativePath: String) =
        withContext(Dispatchers.IO) { fs.deleteRelative(project, relativePath) }
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

    override suspend fun searchFiles(project: Project, query: String): List<SearchMatch> =
        withContext(Dispatchers.IO) { fs.searchFiles(project, query) }
}
