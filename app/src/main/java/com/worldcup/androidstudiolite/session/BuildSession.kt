package com.worldcup.androidstudiolite.session

import android.content.Context
import com.worldcup.androidstudiolite.domain.build.RunBuildUseCase
import com.worldcup.androidstudiolite.entities.BuildProgress
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.util.ApkInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BuildSession(
    private val context: Context,
    private val runBuild: RunBuildUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _progress = MutableStateFlow<BuildProgress?>(null)
    val progress = _progress.asStateFlow()

    private var job: Job? = null

    val isRunning: Boolean
        get() = _progress.value is BuildProgress.Running

    fun start(project: Project) {
        if (isRunning) return
        job = scope.launch {
            try {
                runBuild(project).collect { step ->
                    _progress.value = step
                    if (step is BuildProgress.Success) {
                        installApk(step.apkPath)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _progress.value = BuildProgress.Failed(e.message ?: "Build failed")
            }
        }
    }

    fun installApk(path: String) {
        ApkInstaller.install(context, File(path))
    }

    fun cancel() {
        job?.cancel()
        _progress.value = null
    }

    fun clear() {
        if (!isRunning) _progress.value = null
    }
}
