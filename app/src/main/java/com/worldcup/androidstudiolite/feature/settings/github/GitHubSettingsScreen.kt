package com.worldcup.androidstudiolite.feature.settings.github

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.selectable.AslSwitch
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun GitHubSettingsScreen(
    viewModel: GitHubSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: GitHubSettingsInteractionListener = viewModel
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AslTopBar(
                title = "Connect GitHub",
                navigation = {
                    AslIconButton(AslIcons.ArrowBack, onClick = onBack, contentDescription = "Back")
                },
            )

            Column(
                Modifier.padding(AslTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                if (state.connected) {
                    ConnectedCard(state, listener)
                } else {
                    ConnectCard(state, listener) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/settings/tokens".toUri()),
                        )
                    }
                }
                RepoVisibilityCard(state, listener)
            }
        }

        AslSnackbarHost(snackBar)
    }
}

@Composable
private fun ConnectCard(
    state: GitHubSettingsUiState,
    listener: GitHubSettingsInteractionListener,
    onGetToken: () -> Unit,
) {
    AslCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            AslIcon(AslIcons.GitHub, size = 28.dp)
            AslText("Personal access token", style = AslTheme.typography.title)
        }
        Spacer(Modifier.height(AslTheme.spacing.sm))
        AslText(
            "Studio Lite pushes your projects to GitHub repositories and " +
                "builds them with GitHub Actions. Use a classic token with the " +
                "\"repo\" and \"workflow\" scopes.",
            color = AslTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslTextField(
            value = state.tokenInput,
            onValueChange = listener::onTokenChange,
            label = "Token",
            placeholder = "ghp_…",
            errorText = state.error,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslPrimaryButton(
            "Verify & Connect",
            onClick = listener::onConnect,
            enabled = state.tokenInput.isNotBlank(),
            loading = state.verifying,
            modifier = Modifier.fillMaxWidth(),
        )
        AslTextButton("Get a token on github.com", onClick = onGetToken)
    }
}

@Composable
private fun RepoVisibilityCard(
    state: GitHubSettingsUiState,
    listener: GitHubSettingsInteractionListener,
) {
    AslCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            Column(Modifier.weight(1f)) {
                AslText("Private repositories", style = AslTheme.typography.title)
                AslText(
                    "New project repositories are created private. Turn off if your " +
                        "account can't create private repositories — projects will be " +
                        "pushed to public repos instead.",
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
            AslSwitch(
                checked = state.privateRepos,
                onCheckedChange = listener::onPrivateReposChange,
            )
        }
    }
}

@Composable
private fun ConnectedCard(
    state: GitHubSettingsUiState,
    listener: GitHubSettingsInteractionListener,
) {
    AslCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            AslIcon(AslIcons.CheckCircle, tint = AslTheme.colors.primary, size = 28.dp)
            Column(Modifier.weight(1f)) {
                AslText("Connected", style = AslTheme.typography.title)
                AslText(
                    "Signed in as ${state.owner}",
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslGhostButton("Disconnect", onClick = listener::onDisconnect)
    }
}
