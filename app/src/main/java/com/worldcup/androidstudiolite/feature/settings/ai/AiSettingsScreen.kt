package com.worldcup.androidstudiolite.feature.settings.ai

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.cards.AslInnerCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslCircularProgress
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor

@Composable
fun AiSettingsScreen(
    viewModel: AiSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: AiSettingsInteractionListener = viewModel
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AslTopBar(
                title = "Configure AI",
                navigation = {
                    AslIconButton(AslIcons.ArrowBack, onClick = onBack, contentDescription = "Back")
                },
            )

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = AslTheme.spacing.gutter,
                    vertical = AslTheme.spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                items(state.providers, key = { it.id }) { provider ->
                    ProviderCard(
                        provider = provider,
                        state = state,
                        listener = listener,
                        onGetKey = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, provider.apiKeyUrl.toUri()),
                            )
                        },
                    )
                }

                if (state.connectedProviderId != null) {
                    item { ModelPicker(state, listener) }
                }
            }
        }

        AslSnackbarHost(snackBar)
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderDescriptor,
    state: AiSettingsUiState,
    listener: AiSettingsInteractionListener,
    onGetKey: () -> Unit,
) {
    val selected = provider.id == state.selectedProviderId
    val connected = provider.id == state.connectedProviderId

    AslCard(
        Modifier.fillMaxWidth(),
        onClick = { listener.onSelectProvider(provider.id) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
        ) {
            AslIcon(
                AslIcons.Sparkle,
                tint = if (connected) AslTheme.colors.secondary else AslTheme.colors.onSurfaceVariant,
            )
            AslText(
                provider.displayName,
                style = AslTheme.typography.title,
                modifier = Modifier.weight(1f),
            )
            when {
                connected -> AslStatusChip("Connected", AslStatus.Success)
                state.keyStatus == KeyStatus.Checking && selected ->
                    AslCircularProgress(size = 16.dp)
            }
        }

        if (selected) {
            Spacer(Modifier.height(AslTheme.spacing.md))
            AslTextField(
                value = state.keyInput,
                onValueChange = listener::onKeyChange,
                label = if (connected) "Replace API key" else "API key",
                placeholder = "Paste your ${provider.displayName} API key",
                errorText = if (state.keyStatus == KeyStatus.Invalid) state.keyError else null,
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    when (state.keyStatus) {
                        KeyStatus.Checking -> AslCircularProgress(size = 16.dp)
                        KeyStatus.Valid -> AslIcon(
                            AslIcons.CheckCircle,
                            tint = AslTheme.colors.primary,
                            size = 16.dp,
                        )
                        else -> {}
                    }
                },
            )
            AslTextButton("Get an API key", onClick = onGetKey)
        }
    }
}

@Composable
private fun ModelPicker(state: AiSettingsUiState, listener: AiSettingsInteractionListener) {
    AslCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AslText("Model", style = AslTheme.typography.title, modifier = Modifier.weight(1f))
            if (state.loadingModels) AslCircularProgress(size = 16.dp)
        }
        AslText(
            "Fetched live from the provider — new models appear automatically.",
            style = AslTheme.typography.uiLabelSmall,
            color = AslTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AslTheme.spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.xs)) {
            state.models.forEach { model ->
                val active = model.id == state.activeModelId
                AslInnerCard(
                    Modifier.fillMaxWidth(),
                    onClick = { listener.onSelectModel(model.id) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
                    ) {
                        Column(Modifier.weight(1f)) {
                            AslText(model.displayName, style = AslTheme.typography.uiHeader)
                            AslText(
                                model.id,
                                style = AslTheme.typography.codeSmall,
                                color = AslTheme.colors.onSurfaceVariant,
                            )
                        }
                        if (active) {
                            AslIcon(AslIcons.Check, tint = AslTheme.colors.primary, size = 16.dp)
                        }
                    }
                }
            }
        }
    }
}
