package com.worldcup.androidstudiolite.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.cards.AslInnerCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToGitHub: () -> Unit,
    onNavigateToAi: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        AslTopBar(title = "Settings")

        Column(
            Modifier.padding(AslTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            SettingsRow(
                iconRes = AslIcons.GitHub,
                title = "GitHub",
                subtitle = if (state.githubConnected) "Connected as ${state.githubOwner}" else "Not connected",
                status = if (state.githubConnected) AslStatus.Success else AslStatus.Neutral,
                onClick = onNavigateToGitHub,
            )
            SettingsRow(
                iconRes = AslIcons.Sparkle,
                title = "AI Agent",
                subtitle = state.agentLabel ?: "Not configured",
                status = if (state.agentLabel != null) AslStatus.Success else AslStatus.Neutral,
                onClick = onNavigateToAi,
            )

            AslText(
                "Projects are built in the cloud with GitHub Actions and installed " +
                    "back on this device. The AI assistant uses your own API key.",
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = AslTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    status: AslStatus,
    onClick: () -> Unit,
) {
    AslInnerCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            AslIcon(iconRes, tint = AslTheme.colors.onSurface)
            Column(Modifier.weight(1f)) {
                AslText(title, style = AslTheme.typography.uiHeader)
                AslText(
                    subtitle,
                    style = AslTheme.typography.uiLabelSmall,
                    color = AslTheme.colors.onSurfaceVariant,
                )
            }
            if (status != AslStatus.Neutral) {
                AslStatusChip("●", status)
            }
            AslIcon(AslIcons.ChevronRight, tint = AslTheme.colors.onSurfaceVariant)
        }
    }
}
