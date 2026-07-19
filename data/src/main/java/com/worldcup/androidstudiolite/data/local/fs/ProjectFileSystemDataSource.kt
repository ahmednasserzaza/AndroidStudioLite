package com.worldcup.androidstudiolite.data.local.fs

import android.content.Context
import com.worldcup.androidstudiolite.data.local.templates.ProjectTemplates
import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.entities.ChangeStatus
import com.worldcup.androidstudiolite.entities.FileChange
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.entities.SearchMatch
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

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
                ?.filter {
                    it.name !in HIDDEN_DIRS && it.name != ProjectTemplates.META_FILE &&
                        it.name != SYNC_FILE
                }
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

    fun setBranch(project: Project, branch: String): Project {
        val updated = project.copy(branch = branch)
        writeMeta(updated)
        return updated
    }

    fun recordSynced(project: Project) {
        val json = JSONObject()
        computeHashes(project).forEach { (path, hash) -> json.put(path, hash) }
        File(project.path, SYNC_FILE).writeText(json.toString())
    }

    fun localChanges(project: Project): List<FileChange> {
        val current = computeHashes(project)
        val synced = readSyncedHashes(project)
        val changes = mutableListOf<FileChange>()
        current.forEach { (path, hash) ->
            when {
                path !in synced -> changes += FileChange(path, ChangeStatus.Added)
                synced[path] != hash -> changes += FileChange(path, ChangeStatus.Modified)
            }
        }
        synced.keys.filter { it !in current }.forEach {
            changes += FileChange(it, ChangeStatus.Deleted)
        }
        return changes.sortedBy { it.relativePath }
    }

    fun clearWorkingTree(project: Project) {
        File(project.path).listFiles()?.forEach { child ->
            if (child.name !in HIDDEN_DIRS &&
                child.name != ProjectTemplates.META_FILE &&
                child.name != SYNC_FILE
            ) {
                child.deleteRecursively()
            }
        }
    }

    fun restoreFile(project: Project, relativePath: String, bytes: ByteArray) {
        val target = File(project.path, relativePath)
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
    }

    fun deleteRelative(project: Project, relativePath: String) {
        File(project.path, relativePath).deleteRecursively()
    }

    private fun computeHashes(project: Project): Map<String, String> {
        val projectDir = File(project.path)
        val digest = MessageDigest.getInstance("SHA-1")
        return projectDir.walkTopDown()
            .onEnter { it.name !in HIDDEN_DIRS }
            .filter {
                it.isFile && it.name != ProjectTemplates.META_FILE && it.name != SYNC_FILE
            }
            .associate { file ->
                digest.reset()
                val hash = digest.digest(file.readBytes())
                    .joinToString("") { b -> "%02x".format(b) }
                file.relativeTo(projectDir).invariantSeparatorsPath to hash
            }
    }

    private fun readSyncedHashes(project: Project): Map<String, String> {
        val file = File(project.path, SYNC_FILE)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val json = JSONObject(file.readText())
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun searchFiles(project: Project, query: String): List<SearchMatch> {
        val projectDir = File(project.path)
        val matches = mutableListOf<SearchMatch>()
        val files = projectDir.walkTopDown()
            .onEnter { it.name !in HIDDEN_DIRS }
            .filter { file ->
                file.isFile && file.name != ProjectTemplates.META_FILE &&
                    file.name != SYNC_FILE &&
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
            val name = json.getString("name")
            val storedRepo = json.getString("repoName")
            val legacyRepo = "asl-${sanitize(name)}"
            val repoName = if (storedRepo == legacyRepo) {
                CreateProjectUseCase.repoName(name)
            } else {
                storedRepo
            }
            val project = Project(
                id = dir.name,
                name = name,
                packageName = json.getString("packageName"),
                repoName = repoName,
                path = dir.absolutePath,
                lastModifiedEpochMs = dir.lastModified(),
                isPrivate = json.optBoolean("private", true),
                branch = json.optString("branch", "main").ifBlank { "main" },
            )
            if (storedRepo == legacyRepo) writeMeta(project)
            project
        }.getOrNull()
    }

    companion object {
        const val SYNC_FILE = ".aslsync.json"
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
