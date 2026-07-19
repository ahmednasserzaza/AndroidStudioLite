package com.worldcup.androidstudiolite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.domain.project.GetLastProjectUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveOnboardingUseCase
import com.worldcup.androidstudiolite.navigation.AslApp
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val observeOnboarding: ObserveOnboardingUseCase by inject()
    private val getLastProject: GetLastProjectUseCase by inject()
    private val workspace: WorkspaceSession by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val onboarded = runBlocking { observeOnboarding().first() }

        val resumeInEditor = if (onboarded && workspace.currentProject.value == null) {
            val lastProject = runBlocking { runCatching { getLastProject() }.getOrNull() }
            lastProject?.also { workspace.openProject(it) } != null
        } else {
            workspace.currentProject.value != null
        }
        setContent {
            AslTheme {
                AslApp(onboarded = onboarded, startInEditor = resumeInEditor)
            }
        }
    }
}
