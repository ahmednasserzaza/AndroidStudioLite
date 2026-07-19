package com.worldcup.androidstudiolite.data.remote.github

import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.entities.PushResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.Base64
import java.util.zip.ZipInputStream

class GitHubDataSource(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun api(
        method: HttpMethod,
        url: String,
        body: JsonObject? = null,
    ): JsonObject {
        var attempt = 0
        while (true) {
            val response = client.request(if (url.startsWith("http")) url else "$API$url") {
                this.method = method
                commonHeaders()
                if (method != HttpMethod.Get) {
                    setBody(TextContent((body ?: EMPTY).toString(), ContentType.Application.Json))
                }
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                android.util.Log.w(
                    "GitHubApi",
                    "HTTP ${response.status.value} ${method.value} $url " +
                        "reqId=${response.headers["x-github-request-id"]} body=${text.take(120)}",
                )
            }
            if (response.status.isSuccess()) {
                return when {
                    text.isBlank() -> EMPTY
                    text.trimStart().startsWith("[") -> buildJsonObject {
                        put("items", json.parseToJsonElement(text).jsonArray)
                    }
                    else -> json.parseToJsonElement(text).jsonObject
                }
            }
            val status = response.status.value
            if (status in 500..599 && method == HttpMethod.Get && attempt < MAX_TRANSIENT_RETRIES) {
                attempt++
                delay(TRANSIENT_RETRY_DELAY_MS * attempt)
                continue
            }
            if (status in 500..599) {
                throw DomainException.GitHub(
                    "GitHub's API is having problems right now (HTTP $status). " +
                        "Check githubstatus.com and try again in a few minutes.",
                )
            }
            val message = runCatching {
                json.parseToJsonElement(text).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull()
            throw DomainException.GitHub(
                "GitHub $status: ${message ?: text} (${method.value} $url)",
            )
        }
    }

    private suspend fun tryApi(method: HttpMethod, url: String, body: JsonObject? = null): JsonObject? =
        try {
            api(method, url, body)
        } catch (e: DomainException.GitHub) {
            if (e.message?.contains("GitHub 404") == true) null else throw e
        }

    private suspend fun io.ktor.client.request.HttpRequestBuilder.commonHeaders() {
        header("Authorization", "Bearer ${tokenProvider()}")
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    suspend fun fetchLogin(token: String? = null): String = withContext(Dispatchers.IO) {
        val bearer = token ?: tokenProvider()
        if (bearer.isBlank()) {
            throw DomainException.Auth("Connect your GitHub account in Settings first")
        }
        val response = client.request("$API/user") {
            method = HttpMethod.Get
            header("Authorization", "Bearer $bearer")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
        val status = response.status.value
        if (!response.status.isSuccess()) {
            android.util.Log.w(
                "GitHubApi",
                "HTTP $status GET /user reqId=${response.headers["x-github-request-id"]} " +
                    "body=${response.bodyAsText().take(120)}",
            )
            throw DomainException.Auth(
                when {
                    status == 401 -> "GitHub rejected this token"
                    status in 500..599 ->
                        "GitHub's API is having problems right now (HTTP $status). " +
                            "Your token is probably fine — check githubstatus.com and " +
                            "try connecting again in a few minutes."
                    else -> "GitHub error $status"
                },
            )
        }
        json.parseToJsonElement(response.bodyAsText())
            .jsonObject.getValue("login").jsonPrimitive.content
    }

    suspend fun ensureRepo(
        owner: String,
        repo: String,
        private: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        if (tryApi(HttpMethod.Get, "/repos/$owner/$repo") != null) return@withContext
        try {
            api(
                HttpMethod.Post, "/user/repos",
                buildJsonObject {
                    put("name", repo)
                    put("private", private)
                    put("auto_init", true)
                    put("description", "Created by Android Studio Lite")
                },
            )
        } catch (e: DomainException.GitHub) {
            if (e.message?.contains("GitHub 403") == true) {
                val privateHint = if (private) {
                    " If your account can't create more private repositories, turn off " +
                        "\"Private repositories\" in Settings → GitHub."
                } else {
                    ""
                }
                throw DomainException.Auth(
                    "Your token can't create repositories. Use a CLASSIC personal access " +
                        "token with the \"repo\" and \"workflow\" scopes (GitHub → Settings → " +
                        "Developer settings → Tokens (classic)). Fine-grained tokens don't " +
                        "work here unless granted Administration access.$privateHint",
                )
            }
            throw e
        }
        repeat(15) {
            if (tryApi(HttpMethod.Get, "/repos/$owner/$repo/git/ref/heads/$BRANCH") != null) {
                return@withContext
            }
            delay(1000)
        }
        throw DomainException.GitHub("Repository was created but its default branch never appeared")
    }

    suspend fun pushProject(
        owner: String,
        repo: String,
        projectDir: File,
        excludeName: String,
        message: String,
        force: Boolean = false,
        branch: String = BRANCH,
    ): PushResult = withContext(Dispatchers.IO) {
        val headSha = api(HttpMethod.Get, "/repos/$owner/$repo/git/ref/heads/$branch")
            .getValue("object").jsonObject.getValue("sha").jsonPrimitive.content

        val entries = buildJsonArray {
            collectFiles(projectDir, excludeName).forEach { file ->
                val path = file.relativeTo(projectDir).invariantSeparatorsPath
                add(
                    buildJsonObject {
                        put("path", path)
                        put("mode", "100644")
                        put("type", "blob")
                        if (isBinary(file)) {
                            val blobSha = api(
                                HttpMethod.Post, "/repos/$owner/$repo/git/blobs",
                                buildJsonObject {
                                    put("content", Base64.getEncoder().encodeToString(file.readBytes()))
                                    put("encoding", "base64")
                                },
                            ).getValue("sha").jsonPrimitive.content
                            put("sha", blobSha)
                        } else {
                            put("content", file.readText())
                        }
                    },
                )
            }
        }

        val treeSha = api(
            HttpMethod.Post, "/repos/$owner/$repo/git/trees",
            buildJsonObject { put("tree", entries) },
        ).getValue("sha").jsonPrimitive.content

        val headTreeSha = api(HttpMethod.Get, "/repos/$owner/$repo/git/commits/$headSha")
            .getValue("tree").jsonObject.getValue("sha").jsonPrimitive.content
        if (treeSha == headTreeSha && !force) {
            return@withContext PushResult(commitSha = headSha, pushed = false)
        }

        val commitSha = api(
            HttpMethod.Post, "/repos/$owner/$repo/git/commits",
            buildJsonObject {
                put("message", message)
                put("tree", treeSha)
                put("parents", buildJsonArray { add(headSha) })
            },
        ).getValue("sha").jsonPrimitive.content

        api(
            HttpMethod.Patch, "/repos/$owner/$repo/git/refs/heads/$branch",
            buildJsonObject {
                put("sha", commitSha)
                put("force", true)
            },
        )
        PushResult(commitSha = commitSha, pushed = true)
    }

    private fun collectFiles(root: File, excludeName: String): List<File> =
        root.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "build" && it.name != ".gradle" }
            .filter { it.isFile && it.name != excludeName }
            .toList()

    private fun isBinary(file: File): Boolean =
        file.extension.lowercase() in
            setOf("keystore", "jks", "png", "jpg", "jpeg", "webp", "jar", "so", "zip")

    suspend fun findRun(owner: String, repo: String, commitSha: String): JsonObject? =
        withContext(Dispatchers.IO) {
            val runs = api(HttpMethod.Get, "/repos/$owner/$repo/actions/runs?head_sha=$commitSha")
                .getValue("workflow_runs").jsonArray
            if (runs.isEmpty()) null else runs[0].jsonObject
        }

    suspend fun getRun(owner: String, repo: String, runId: Long): JsonObject =
        withContext(Dispatchers.IO) {
            api(HttpMethod.Get, "/repos/$owner/$repo/actions/runs/$runId")
        }

    suspend fun listJobs(owner: String, repo: String, runId: Long): JsonArray =
        withContext(Dispatchers.IO) {
            api(HttpMethod.Get, "/repos/$owner/$repo/actions/runs/$runId/jobs")
                .getValue("jobs").jsonArray
        }

    suspend fun <T> readJobLog(
        owner: String,
        repo: String,
        jobId: Long,
        consume: (Sequence<String>) -> T,
    ): T? = withContext(Dispatchers.IO) {
        client.prepareGet("$API/repos/$owner/$repo/actions/jobs/$jobId/logs") {
            header("Authorization", "Bearer ${tokenProvider()}")
        }.execute { response: HttpResponse ->
            if (!response.status.isSuccess()) return@execute null
            response.bodyAsChannel().toInputStream().bufferedReader().useLines { lines ->
                consume(lines)
            }
        }
    }

    suspend fun downloadApkArtifact(owner: String, repo: String, runId: Long, destDir: File): File =
        withContext(Dispatchers.IO) {
            val artifacts = api(HttpMethod.Get, "/repos/$owner/$repo/actions/runs/$runId/artifacts")
                .getValue("artifacts").jsonArray
            if (artifacts.isEmpty()) {
                throw DomainException.GitHub("Build finished but produced no artifacts")
            }
            val artifactId = artifacts[0].jsonObject.getValue("id").jsonPrimitive.content

            client.prepareGet("$API/repos/$owner/$repo/actions/artifacts/$artifactId/zip") {
                header("Authorization", "Bearer ${tokenProvider()}")
                header("Accept", "application/vnd.github+json")
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw DomainException.GitHub("Artifact download failed: HTTP ${response.status.value}")
                }
                destDir.mkdirs()
                ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            val out = File(destDir, File(entry.name).name)
                            out.outputStream().use { zip.copyTo(it) }
                            return@execute out
                        }
                        entry = zip.nextEntry
                    }
                }
                throw DomainException.GitHub("Artifact did not contain an APK")
            }
        }

    suspend fun pullProject(owner: String, repo: String, projectDir: File, branch: String = BRANCH): Int =
        withContext(Dispatchers.IO) {
            client.prepareGet("$API/repos/$owner/$repo/zipball/$branch") {
                header("Authorization", "Bearer ${tokenProvider()}")
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw DomainException.GitHub("Pull failed: HTTP ${response.status.value}")
                }
                var count = 0
                ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val relative = entry.name.substringAfter('/', "")
                            if (relative.isNotEmpty()) {
                                val target = File(projectDir, relative)
                                if (!target.canonicalPath.startsWith(projectDir.canonicalPath)) {
                                    throw DomainException.GitHub(
                                        "Refusing to extract outside the project: ${entry.name}",
                                    )
                                }
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                                count++
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
                count
            }
        }

    suspend fun listUserRepos(limit: Int = 100): JsonArray = withContext(Dispatchers.IO) {
        api(HttpMethod.Get, "/user/repos?per_page=$limit&sort=updated")
            .getValue("items").jsonArray
    }

    suspend fun listCommits(owner: String, repo: String, limit: Int): JsonArray =
        withContext(Dispatchers.IO) {
            tryApi(HttpMethod.Get, "/repos/$owner/$repo")
                ?: return@withContext buildJsonArray { }
            try {
                api(HttpMethod.Get, "/repos/$owner/$repo/commits?per_page=$limit")
                    .getValue("items").jsonArray
            } catch (e: DomainException.GitHub) {
                if ("GitHub 409" in e.message.orEmpty()) buildJsonArray { } else throw e
            }
        }

    companion object {
        private const val API = "https://api.github.com"
        const val BRANCH = "main"
        private const val MAX_TRANSIENT_RETRIES = 2
        private const val TRANSIENT_RETRY_DELAY_MS = 1_000L
        private val EMPTY = buildJsonObject { }
    }
}
