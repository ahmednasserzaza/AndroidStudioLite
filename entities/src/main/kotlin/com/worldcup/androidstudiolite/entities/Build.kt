package com.worldcup.androidstudiolite.entities

enum class RunStatus { Queued, InProgress, Completed, Unknown }

enum class RunConclusion { Success, Failure, Cancelled, Other }

data class WorkflowRun(
    val id: Long,
    val status: RunStatus,
    val conclusion: RunConclusion?,
    val htmlUrl: String,
)

enum class BuildStep { Connect, PrepareRepo, Push, WaitForCi, Compile, Download }

sealed interface BuildProgress {
    data class Running(val step: BuildStep, val detail: String = "") : BuildProgress
    data class Success(val apkPath: String, val runUrl: String) : BuildProgress
    data class Failed(
        val message: String,
        val runUrl: String? = null,
        val errorLines: List<String> = emptyList(),
        val step: BuildStep? = null,
        val diagnostics: List<BuildDiagnostic> = emptyList(),
    ) : BuildProgress
}

/** A compiler/build error parsed from CI logs, pointing at a project file location. */
data class BuildDiagnostic(
    val relativePath: String,
    val line: Int,
    val column: Int,
    val message: String,
)
