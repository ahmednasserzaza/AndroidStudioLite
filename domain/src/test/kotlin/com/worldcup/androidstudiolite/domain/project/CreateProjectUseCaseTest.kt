package com.worldcup.androidstudiolite.domain.project

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.Project
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CreateProjectUseCaseTest {

    private val repository = object : ProjectRepository {
        override suspend fun listProjects(): List<Project> = emptyList()
        override suspend fun createProject(name: String, packageName: String) = Project(
            id = CreateProjectUseCase.sanitize(name),
            name = name,
            packageName = packageName,
            repoName = "asl-${CreateProjectUseCase.sanitize(name)}",
            path = "/projects/${CreateProjectUseCase.sanitize(name)}",
            lastModifiedEpochMs = 0L,
        )
        override suspend fun deleteProject(project: Project) = Unit
        override suspend fun repairInfrastructure(project: Project) = Unit
    }

    private val useCase = CreateProjectUseCase(repository)

    @Test
    fun `creates project with valid input`() = runBlocking {
        val project = useCase("My App", "com.example.myapp")
        assertEquals("my-app", project.id)
        assertEquals("com.example.myapp", project.packageName)
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
}
