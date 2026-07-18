package com.worldcup.androidstudiolite.data.local.fs

import android.content.Context
import com.worldcup.androidstudiolite.data.local.templates.ProjectTemplates
import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.SearchMatch
import org.json.JSONObject
import java.io.File

class ProjectFileSystemDataSource(context: Context) {

    private val root = File(context.filesDir, "projects").apply { mkdirs() }
    val apkCacheDir: File = File(context.cacheDir, "apks")

    fun listProjects(): List<Project> =
        root.listFiles { f -> f.isDirectory }
            ?.mapNotNull { dir -> readMeta(dir) }
            ?.sortedByDescending { it.lastModifiedEpochMs }
            ?: emptyList()

    fun createProject(name: String, packageName: String, isPrivate: Boolean): Project {
        val dirName = sanitize(name)
        val dir = File(root, dirName)
        if (dir.exists()) throw DomainException.Validation("A project named \"$name\" already exists")
        dir.mkdirs()
        val project = Project(
            id = dirName,
            name = name,
            packageName = packageName,
            repoName = CreateProjectUseCase.repoName(name),
            path = dir.absolutePath,
            lastModifiedEpochMs = dir.lastModified(),
            isPrivate = isPrivate,
        )
        ProjectTemplates.writeNewProject(project)
        writeMeta(project)
        return project
    }

    fun createImportShell(repo: RemoteRepo): Project {
        val dirName = sanitize(repo.name)
        val dir = File(root, dirName)
        if (dir.exists()) {
            throw DomainException.Validation("A project named \"${repo.name}\" already exists")
        }
        dir.mkdirs()
        val project = Project(
            id = dirName,
            name = repo.name,
            packageName = "",
            repoName = repo.name,
            path = dir.absolutePath,
            lastModifiedEpochMs = dir.lastModified(),
            isPrivate = repo.isPrivate,
            branch = repo.defaultBranch.ifBlank { "main" },
        )
        writeMeta(project)
        return project
    }

    fun finalizeImport(project: Project): Project {
        val detected = detectPackageName(File(project.path)) ?: "com.example.app"
        val updated = project.copy(packageName = detected)
        writeMeta(updated)
        return updated
    }

    /** Best-effort applicationId/namespace detection from the pulled Gradle files. */
    private fun detectPackageName(projectDir: File): String? {
        val gradleFiles = projectDir.walkTopDown()
            .onEnter { it.name !in HIDDEN_DIRS }
            .filter { it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts") }
        for (file in gradleFiles) {
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            val match = APPLICATION_ID_REGEX.find(text) ?: NAMESPACE_REGEX.find(text)
            if (match != null) return match.groupValues[2]
        }
        return null
    }

    fun deleteProject(project: Project) {
        File(project.path).deleteRecursively()
    }

    fun listFiles(project: Project): List<FileNode> {
        val projectDir = File(project.path)
        val nodes = mutableListOf<FileNode>()
        fun visit(dir: File, depth: Int) {
            val children = dir.listFiles()
                ?.filter { it.name !in HIDDEN_DIRS && it.name != ProjectTemplates.META_FILE }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: return
            for (child in children) {
                nodes += FileNode(
                    path = child.absolutePath,
                    relativePath = child.relativeTo(projectDir).invariantSeparatorsPath,
                    name = child.name,
                    isDirectory = child.isDirectory,
                    depth = depth,
                )
                if (child.isDirectory) visit(child, depth + 1)
            }
        }
        visit(projectDir, 0)
        return nodes
    }

    fun readFile(path: String): String = File(path).readText()

    fun writeFile(path: String, content: String) {
        File(path).writeText(repairText(content))
    }

    fun createEntry(parentPath: String, name: String, isDirectory: Boolean): FileNode {
        val target = File(parentPath, name)
        if (target.exists()) throw DomainException.Validation("\"$name\" already exists")
        if (isDirectory) {
            target.mkdirs()
        } else {
            target.parentFile?.mkdirs()
            target.writeText("")
        }
        return FileNode(
            path = target.absolutePath,
            relativePath = target.name,
            name = target.name,
            isDirectory = isDirectory,
            depth = 0,
        )
    }

    fun rename(path: String, newName: String): String {
        val file = File(path)
        val target = File(file.parentFile, newName)
        if (target.exists()) throw DomainException.Validation("\"$newName\" already exists")
        if (!file.renameTo(target)) throw DomainException.Validation("Couldn't rename ${file.name}")
        return target.absolutePath
    }

    fun delete(path: String) {
        File(path).deleteRecursively()
    }

    fun searchFiles(project: Project, query: String): List<SearchMatch> {
        val projectDir = File(project.path)
        val matches = mutableListOf<SearchMatch>()
        val files = projectDir.walkTopDown()
            .onEnter { it.name !in HIDDEN_DIRS }
            .filter { file ->
                file.isFile && file.name != ProjectTemplates.META_FILE &&
                    file.length() <= MAX_SEARCHABLE_BYTES &&
                    (file.extension.lowercase() in TEXT_EXTENSIONS || file.name == ".gitignore")
            }
        outer@ for (file in files) {
            val lines = runCatching { file.readLines() }.getOrNull() ?: continue
            lines.forEachIndexed { index, line ->
                val column = line.indexOf(query, ignoreCase = true)
                if (column >= 0) {
                    matches += SearchMatch(
                        path = file.absolutePath,
                        relativePath = file.relativeTo(projectDir).invariantSeparatorsPath,
                        fileName = file.name,
                        lineNumber = index + 1,
                        lineText = line.trim().take(MAX_PREVIEW_CHARS),
                        columnStart = column,
                    )
                }
            }
            if (matches.size >= MAX_SEARCH_RESULTS) break@outer
        }
        return matches.take(MAX_SEARCH_RESULTS)
    }

    fun repairInfrastructure(project: Project) {
        val projectDir = File(project.path)
        ProjectTemplates.writeWorkflow(projectDir)
        ProjectTemplates.writeKeystore(projectDir)
        projectDir.walkTopDown()
            .onEnter { it.name !in HIDDEN_DIRS }
            .filter { it.isFile && it.extension.lowercase() in TEXT_EXTENSIONS }
            .forEach { file ->
                val original = file.readText()
                val repaired = repairText(original)
                if (repaired != original) file.writeText(repaired)
            }
    }

    private fun repairText(content: String): String = content
        .replace('“', '"').replace('”', '"')
        .replace('‘', '\'').replace('’', '\'')
        .replace(' ', ' ')

    private fun writeMeta(project: Project) {
        val json = JSONObject()
            .put("name", project.name)
            .put("packageName", project.packageName)
            .put("repoName", project.repoName)
            .put("private", project.isPrivate)
            .put("branch", project.branch)
        File(project.path, ProjectTemplates.META_FILE).writeText(json.toString(2))
    }

    private fun readMeta(dir: File): Project? {
        val metaFile = File(dir, ProjectTemplates.META_FILE)
        if (!metaFile.exists()) return null
        return runCatching {
            val json = JSONObject(metaFile.readText())
            Project(
                id = dir.name,
                name = json.getString("name"),
                packageName = json.getString("packageName"),
                repoName = json.getString("repoName"),
                path = dir.absolutePath,
                lastModifiedEpochMs = dir.lastModified(),
                isPrivate = json.optBoolean("private", true),
                branch = json.optString("branch", "main").ifBlank { "main" },
            )
        }.getOrNull()
    }

    companion object {
        val HIDDEN_DIRS = setOf(".git", "build", ".gradle", ".idea")
        private val TEXT_EXTENSIONS =
            setOf("kt", "kts", "java", "xml", "yml", "yaml", "json", "properties", "md", "txt")
        private const val MAX_SEARCHABLE_BYTES = 512 * 1024L
        private const val MAX_SEARCH_RESULTS = 200
        private const val MAX_PREVIEW_CHARS = 160
        private val APPLICATION_ID_REGEX =
            Regex("""applicationId\s*(=\s*)?["']([\w.]+)["']""")
        private val NAMESPACE_REGEX =
            Regex("""namespace\s*(=\s*)?["']([\w.]+)["']""")

        fun sanitize(name: String): String =
            name.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "project" }
    }
}
