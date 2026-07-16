package com.worldcup.androidstudiolite.data.local.fs

import android.content.Context
import com.worldcup.androidstudiolite.data.local.templates.ProjectTemplates
import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.entities.FileNode
import com.worldcup.androidstudiolite.entities.Project
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

    fun createProject(name: String, packageName: String): Project {
        val dirName = sanitize(name)
        val dir = File(root, dirName)
        if (dir.exists()) throw DomainException.Validation("A project named \"$name\" already exists")
        dir.mkdirs()
        val project = Project(
            id = dirName,
            name = name,
            packageName = packageName,
            repoName = "asl-$dirName",
            path = dir.absolutePath,
            lastModifiedEpochMs = dir.lastModified(),
        )
        ProjectTemplates.writeNewProject(project)
        writeMeta(project)
        return project
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
            )
        }.getOrNull()
    }

    companion object {
        val HIDDEN_DIRS = setOf(".git", "build", ".gradle", ".idea")
        private val TEXT_EXTENSIONS =
            setOf("kt", "kts", "java", "xml", "yml", "yaml", "json", "properties", "md", "txt")

        fun sanitize(name: String): String =
            name.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "project" }
    }
}
