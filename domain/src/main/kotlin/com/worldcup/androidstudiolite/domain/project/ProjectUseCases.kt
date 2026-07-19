package com.worldcup.androidstudiolite.domain.project

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import com.worldcup.androidstudiolite.entities.Project
import kotlinx.coroutines.flow.first

class GetProjectsUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(): List<Project> = projects.listProjects()
}

class CreateProjectUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(name: String, packageName: String, isPrivate: Boolean = true): Project {
        if (name.isBlank()) throw DomainException.Validation("Project name can't be empty")
        if (!packageName.matches(PACKAGE_REGEX)) {
            throw DomainException.Validation("\"$packageName\" is not a valid package name")
        }
        return projects.createProject(name.trim(), packageName.trim(), isPrivate)
    }

    companion object {
        private val PACKAGE_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

        fun sanitize(name: String): String =
            name.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "project" }

        fun repoName(name: String): String =
            name.trim()
                .replace(Regex("[^A-Za-z0-9._-]+"), "-")
                .trim('-')
                .ifEmpty { "project" }

        fun defaultPackage(name: String): String =
            "com.example." + sanitize(name).replace("-", "")
    }
}

class DeleteProjectUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(project: Project) = projects.deleteProject(project)
}

class SaveLastProjectUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(project: Project?) = settings.setLastProjectId(project?.id ?: "")
}

class GetLastProjectUseCase(
    private val projects: ProjectRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(): Project? {
        val id = settings.lastProjectId().first()
        if (id.isBlank()) return null
        return projects.listProjects().firstOrNull { it.id == id }
    }
}

class RepairProjectInfrastructureUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(project: Project) = projects.repairInfrastructure(project)
}
