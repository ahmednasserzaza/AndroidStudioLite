package com.worldcup.androidstudiolite.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslInnerCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onConnectGitHub: () -> Unit,
    onConfigureAi: () -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(AslTheme.colors.background)
            .padding(AslTheme.spacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        AslIcon(AslIcons.Android, size = 48.dp, tint = AslTheme.colors.primaryContainer)
        Spacer(Modifier.height(AslTheme.spacing.lg))
        AslText("Welcome to Studio Lite", style = AslTheme.typography.displaySmall)
        Spacer(Modifier.height(AslTheme.spacing.sm))
        AslText(
            "Build real Android apps on your phone. Code locally, compile in the " +
                "cloud, install instantly.",
            color = AslTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AslTheme.spacing.xl))

        AslInnerCard(Modifier.fillMaxWidth(), onClick = onConnectGitHub) {
            StepRow(
                iconRes = AslIcons.GitHub,
                title = "Connect GitHub",
                subtitle = "Required to build & run projects",
                done = state.githubConnected,
            )
        }
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslInnerCard(Modifier.fillMaxWidth(), onClick = onConfigureAi) {
            StepRow(
                iconRes = AslIcons.Sparkle,
                title = "Configure AI",
                subtitle = "Optional — enables the coding assistant",
                done = state.aiConnected,
            )
        }

        Spacer(Modifier.height(AslTheme.spacing.xl))
        AslPrimaryButton(
            "Start building",
            onClick = { viewModel.complete(onDone) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AslTheme.spacing.sm))
        AslGhostButton(
            "Skip for now",
            onClick = { viewModel.complete(onDone) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepRow(iconRes: Int, title: String, subtitle: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
    ) {
        AslIcon(iconRes)
        Column(Modifier.weight(1f)) {
            AslText(title, style = AslTheme.typography.uiHeader)
            AslText(
                subtitle,
                style = AslTheme.typography.uiLabelSmall,
                color = AslTheme.colors.onSurfaceVariant,
            )
        }
        if (done) {
            AslStatusChip("Done", AslStatus.Success)
        } else {
            AslIcon(AslIcons.ChevronRight, tint = AslTheme.colors.onSurfaceVariant)
        }
    }
}
