package com.worldcup.androidstudiolite.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.worldcup.androidstudiolite.designsystem.components.navigation.AslBottomNavBar
import com.worldcup.androidstudiolite.designsystem.components.navigation.AslNavItem
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.feature.build.BuildScreen
import com.worldcup.androidstudiolite.feature.editor.EditorScreen
import com.worldcup.androidstudiolite.feature.onboarding.OnboardingScreen
import com.worldcup.androidstudiolite.feature.projects.ProjectsScreen
import com.worldcup.androidstudiolite.feature.settings.github.GitHubSettingsScreen
import com.worldcup.androidstudiolite.feature.vcs.VcsScreen
import com.worldcup.androidstudiolite.session.BuildSession
import com.worldcup.androidstudiolite.session.WorkspaceSession
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val NAV_ITEMS = listOf(
    AslNavItem("projects", "Project", AslIcons.Folder),
    AslNavItem("editor", "Editor", AslIcons.Code),
    AslNavItem("vcs", "VCS", AslIcons.GitBranch),
)

private fun NavKey.navId(): String? = when (this) {
    ProjectsKey -> "projects"
    EditorKey -> "editor"
    VcsKey -> "vcs"
    else -> null
}

private fun navKeyFor(id: String): NavKey = when (id) {
    "editor" -> EditorKey
    "vcs" -> VcsKey
    else -> ProjectsKey
}

@Composable
fun AslApp(onboarded: Boolean) {
    val backStack = rememberNavBackStack(if (onboarded) ProjectsKey else OnboardingKey)

    fun switchTab(id: String) {
        val key = navKeyFor(id)
        if (backStack.lastOrNull() == key) return
        backStack.clear()
        if (key != ProjectsKey) backStack.add(ProjectsKey)
        backStack.add(key)
    }

    fun push(key: NavKey) {
        if (backStack.lastOrNull() != key) backStack.add(key)
    }

    val currentTop = backStack.lastOrNull()
    val selectedNavId = (currentTop as? NavKey)?.navId()

    Column(
        Modifier
            .fillMaxSize()
            .background(AslTheme.colors.background)
            .statusBarsPadding(),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            modifier = Modifier.weight(1f),
            entryProvider = entryProvider {
                entry<OnboardingKey> {
                    OnboardingScreen(
                        viewModel = koinViewModel(),
                        onConnectGitHub = { push(GitHubSettingsKey) },
                        onDone = {
                            backStack.clear()
                            backStack.add(ProjectsKey)
                        },
                    )
                }
                entry<ProjectsKey> {
                    ProjectsScreen(
                        viewModel = koinViewModel(),
                        onNavigateToEditor = { switchTab("editor") },
                        onNavigateToSettings = { push(GitHubSettingsKey) },
                    )
                }
                entry<EditorKey> {
                    EditorScreen(
                        viewModel = koinViewModel(),
                        onNavigateToBuild = { push(BuildProgressKey) },
                        onNavigateToProjects = { switchTab("projects") },
                    )
                }
                entry<VcsKey> {
                    VcsScreen(
                        viewModel = koinViewModel(),
                        onNavigateToProjects = { switchTab("projects") },
                    )
                }
                entry<GitHubSettingsKey> {
                    GitHubSettingsScreen(
                        viewModel = koinViewModel(),
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<BuildProgressKey> {
                    val buildSession = koinInject<BuildSession>()
                    val workspace = koinInject<WorkspaceSession>()
                    BuildScreen(
                        buildSession = buildSession,
                        workspace = workspace,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )

        if (selectedNavId != null) {
            AslBottomNavBar(
                items = NAV_ITEMS,
                selectedId = selectedNavId,
                onSelect = { switchTab(it.id) },
            )
        }
    }
}
