package com.worldcup.androidstudiolite.domain.project

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateProjectUseCaseTest {

    private val repository = object : ProjectRepository {
        override suspend fun listProjects(): List<Project> = emptyList()
        override suspend fun createProject(name: String, packageName: String, isPrivate: Boolean) =
            Project(
                id = CreateProjectUseCase.sanitize(name),
                name = name,
                packageName = packageName,
                repoName = CreateProjectUseCase.repoName(name),
                path = "/projects/${CreateProjectUseCase.sanitize(name)}",
                lastModifiedEpochMs = 0L,
                isPrivate = isPrivate,
            )
        override suspend fun deleteProject(project: Project) = Unit
        override suspend fun repairInfrastructure(project: Project) = Unit
        override suspend fun createImportShell(repo: RemoteRepo): Project =
            throw UnsupportedOperationException()
        override suspend fun finalizeImport(project: Project): Project = project
        override suspend fun setBranch(project: Project, branch: String): Project =
            project.copy(branch = branch)
        override suspend fun recordSynced(project: Project) = Unit
        override suspend fun localChanges(project: Project) = emptyList<FileChange>()
        override suspend fun clearWorkingTree(project: Project) = Unit
        override suspend fun restoreFile(project: Project, relativePath: String, bytes: ByteArray) = Unit
        override suspend fun deleteFile(project: Project, relativePath: String) = Unit
    }

    private val useCase = CreateProjectUseCase(repository)

    @Test
    fun `creates project with valid input`() = runBlocking {
        val project = useCase("My App", "com.example.myapp")
        assertEquals("my-app", project.id)
        assertEquals("com.example.myapp", project.packageName)
        assertTrue(project.isPrivate)
    }

    @Test
    fun `respects the private flag`() = runBlocking {
        val project = useCase("My App", "com.example.myapp", isPrivate = false)
        assertEquals(false, project.isPrivate)
    }

    @Test
    fun `rejects blank name`() {
        assertThrows(DomainException.Validation::class.java) {
            runBlocking { useCase("  ", "com.example.app") }
        }
    }

    @Test
    fun `rejects invalid package name`() {
        assertThrows(DomainException.Validation::class.java) {
            runBlocking { useCase("App", "NotAPackage") }
        }
    }

    @Test
    fun `sanitize produces safe directory names`() {
        assertEquals("my-cool-app", CreateProjectUseCase.sanitize("My Cool App!"))
        assertEquals("project", CreateProjectUseCase.sanitize("###"))
        assertEquals("com.example.mycoolapp", CreateProjectUseCase.defaultPackage("My Cool App"))
    }

    @Test
    fun `repo name matches the project name as closely as GitHub allows`() {
        assertEquals("Test-App", CreateProjectUseCase.repoName("Test App"))
        assertEquals("MyApp", CreateProjectUseCase.repoName("MyApp"))
        assertEquals("My-App-2.0", CreateProjectUseCase.repoName("My App 2.0"))
        assertEquals("project", CreateProjectUseCase.repoName("###"))
    }
}
