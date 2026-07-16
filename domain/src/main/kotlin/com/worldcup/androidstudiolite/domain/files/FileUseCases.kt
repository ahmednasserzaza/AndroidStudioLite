package com.worldcup.androidstudiolite.domain.files

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.repository.ProjectFilesRepository
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project

class GetFileTreeUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(project: Project): List<FileNode> = files.listFiles(project)
}

class ReadFileUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(path: String): String = files.readFile(path)
}

class SaveFileUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(path: String, content: String) = files.writeFile(path, content)
}

class CreateFileEntryUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(parentPath: String, name: String, isDirectory: Boolean): FileNode {
        if (name.isBlank() || name.contains('/')) {
            throw DomainException.Validation("\"$name\" is not a valid file name")
        }
        return files.createEntry(parentPath, name.trim(), isDirectory)
    }
}

class RenameFileEntryUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(path: String, newName: String): String {
        if (newName.isBlank() || newName.contains('/')) {
            throw DomainException.Validation("\"$newName\" is not a valid name")
        }
        return files.rename(path, newName.trim())
    }
}

class DeleteFileEntryUseCase(private val files: ProjectFilesRepository) {
    suspend operator fun invoke(path: String) = files.delete(path)
}
