package com.worldcup.androidstudiolite.domain.project

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.entities.Project

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

        /**
         * The GitHub repo name for a project: the project name itself, only
         * adjusted where GitHub requires it (no spaces/special characters).
         * "Test App" → "Test-App".
         */
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

class RepairProjectInfrastructureUseCase(private val projects: ProjectRepository) {
    suspend operator fun invoke(project: Project) = projects.repairInfrastructure(project)
}
