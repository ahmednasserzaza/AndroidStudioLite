package com.worldcup.androidstudiolite.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslDangerButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslChip
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.components.dialogs.AslDialog
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslCircularProgress
import com.worldcup.androidstudiolite.designsystem.components.selectable.AslSwitch
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.entities.Project
import com.worldcup.androidstudiolite.entities.RemoteRepo
import com.worldcup.androidstudiolite.feature.base.CollectEffects
import java.text.DateFormat
import java.util.Date

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: ProjectsInteractionListener = viewModel

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            ProjectsScreenEffect.NavigateToEditor -> onNavigateToEditor()
            ProjectsScreenEffect.NavigateToGitHubSettings -> onNavigateToSettings()
        }
    }

    Column(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        AslTopBar(
            title = "Studio Lite",
            actions = {
                AslIconButton(
                    AslIcons.Settings,
                    onClick = onNavigateToSettings,
                    contentDescription = "Settings",
                )
            },
        )
        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = AslTheme.spacing.gutter,
                    vertical = AslTheme.spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                item { WelcomeCard(state, listener) }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = AslTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AslText(
                            "Recent Projects",
                            style = AslTheme.typography.uiHeader,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.githubConnected) {
                            AslTextButton("Import", onClick = { listener.onShowImportDialog(true) })
                        }
                        AslTextButton("New Project", onClick = { listener.onShowCreateDialog(true) })
                    }
                }
                if (!state.loading && state.projects.isEmpty()) {
                    item {
                        AslText(
                            "No projects yet — create one to get started.",
                            color = AslTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = AslTheme.spacing.lg),
                        )
                    }
                }
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(project, listener)
                }
            }

            AslSnackbarHost(snackBar)

            if (state.showCreateDialog) {
                CreateProjectDialog(
                    creating = state.creating,
                    defaultPrivate = state.defaultPrivate,
                    onDismiss = { listener.onShowCreateDialog(false) },
                    onCreate = listener::onCreateProject,
                )
            }

            if (state.showImportDialog) {
                ImportRepoDialog(state = state, listener = listener)
            }

            state.confirmDelete?.let { project ->
                DeleteProjectDialog(project = project, listener = listener)
            }
        }
    }
}

@Composable
private fun WelcomeCard(state: ProjectsScreenState, listener: ProjectsInteractionListener) {
    AslCard(Modifier.fillMaxWidth()) {
        AslText("Welcome to Studio Lite", style = AslTheme.typography.displaySmall)
        Spacer(Modifier.height(AslTheme.spacing.sm))
        AslText(
            "Streamlined mobile-first IDE for Kotlin and Jetpack Compose. " +
                "Sync your repositories and start building instantly.",
            color = AslTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AslTheme.spacing.lg))
        if (state.githubConnected) {
            AslStatusChip("GitHub connected", AslStatus.Success)
        } else {
            AslPrimaryButton(
                text = "Connect with GitHub",
                onClick = listener::onConnectGitHub,
                leadingIconRes = AslIcons.GitHub,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProjectCard(project: Project, listener: ProjectsInteractionListener) {
    AslCard(
        Modifier.fillMaxWidth(),
        color = AslTheme.colors.surfaceContainerLow,
        shape = AslTheme.shapes.large,
        contentPadding = AslTheme.spacing.md,
        onClick = { listener.onOpenProject(project) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(AslTheme.colors.primaryContainer, AslTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                AslText(
                    project.name.firstOrNull()?.uppercase() ?: "P",
                    style = AslTheme.typography.title,
                    color = AslTheme.colors.canvas,
                )
            }
            Column(Modifier.weight(1f)) {
                AslText(
                    project.name,
                    style = AslTheme.typography.uiHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AslText(
                    "Modified " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(project.lastModifiedEpochMs)),
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs)) {
                    AslChip("⎇ ${project.branch}")
                    AslChip("Kotlin")
                    AslChip("Compose")
                }
            }
            AslIconButton(
                AslIcons.Delete,
                onClick = { listener.onRequestDeleteProject(project) },
                tint = AslTheme.colors.onSurfaceVariant,
                contentDescription = "Delete ${project.name}",
            )
        }
    }
}

@Composable
private fun CreateProjectDialog(
    creating: Boolean,
    defaultPrivate: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, isPrivate: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var packageEdited by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(defaultPrivate) }

    AslDialog(
        title = "New Project",
        onDismissRequest = { if (!creating) onDismiss() },
        buttons = {
            AslGhostButton("Cancel", onClick = onDismiss, enabled = !creating)
            AslPrimaryButton(
                "Create",
                onClick = { onCreate(name, packageName, isPrivate) },
                enabled = name.isNotBlank(),
                loading = creating,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md)) {
            AslTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (!packageEdited) packageName = CreateProjectUseCase.defaultPackage(it)
                },
                label = "Project name",
                placeholder = "My App",
            )
            if (name.isNotBlank()) {
                AslText(
                    "GitHub repository: ${CreateProjectUseCase.repoName(name)}",
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
            AslTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    packageEdited = true
                },
                label = "Package name",
                placeholder = "com.example.myapp",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AslText("Private repository", style = AslTheme.typography.uiBody)
                    AslText(
                        "The GitHub repo backing this project is created private",
                        style = AslTheme.typography.uiLabelSmall,
                        color = AslTheme.colors.onSurfaceVariant,
                    )
                }
                AslSwitch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    enabled = !creating,
                )
            }
        }
    }
}

@Composable
private fun DeleteProjectDialog(project: Project, listener: ProjectsInteractionListener) {
    AslDialog(
        title = "Delete ${project.name}?",
        onDismissRequest = listener::onDismissDeleteProject,
        buttons = {
            AslGhostButton("Cancel", onClick = listener::onDismissDeleteProject)
            AslDangerButton("Delete", onClick = { listener.onDeleteProject(project) })
        },
    ) {
        AslText(
            "This removes the project and all its files from this device. " +
                "It can't be undone. The GitHub repository (${project.repoName}) " +
                "is not affected.",
            color = AslTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImportRepoDialog(state: ProjectsScreenState, listener: ProjectsInteractionListener) {

    val importingRepo = state.importingRepo
    if (importingRepo != null) {
        AslDialog(
            title = "Importing $importingRepo",
            onDismissRequest = {},
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                AslCircularProgress(size = 22.dp)
                AslText(
                    "Pulling files from GitHub and setting up the project. " +
                        "This can take a moment on large repositories.",
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    var filter by remember { mutableStateOf("") }
    val repos = state.importableRepos.filter {
        filter.isBlank() || it.name.contains(filter, ignoreCase = true)
    }

    AslDialog(
        title = "Import from GitHub",
        onDismissRequest = { listener.onShowImportDialog(false) },
        buttons = {
            AslGhostButton(
                "Cancel",
                onClick = { listener.onShowImportDialog(false) },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md)) {
            AslTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = "Filter repositories",
            )
            when {
                state.loadingRepos -> Box(
                    Modifier.fillMaxWidth().padding(vertical = AslTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    AslCircularProgress()
                }
                repos.isEmpty() -> AslText(
                    "No repositories to import.",
                    color = AslTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AslTheme.spacing.md),
                )
                else -> LazyColumn(
                    Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs),
                ) {
                    items(repos, key = { it.name }) { repo ->
                        RepoRow(
                            repo = repo,
                            onClick = { listener.onImportRepo(repo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoRow(
    repo: RemoteRepo,
    onClick: () -> Unit,
) {
    AslCard(
        Modifier.fillMaxWidth(),
        color = AslTheme.colors.surfaceContainerLow,
        contentPadding = AslTheme.spacing.md,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                AslText(
                    repo.name,
                    style = AslTheme.typography.uiHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (repo.description.isNotBlank()) {
                    AslText(
                        repo.description,
                        style = AslTheme.typography.uiLabelSmall,
                        color = AslTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (repo.isPrivate) AslChip("Private")
        }
    }
}
