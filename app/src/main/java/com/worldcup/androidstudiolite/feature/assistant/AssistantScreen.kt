package com.worldcup.androidstudiolite.feature.assistant

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslFab
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.components.selectable.AslSwitch
import com.worldcup.androidstudiolite.designsystem.components.sheets.AslBottomSheet
import com.worldcup.androidstudiolite.designsystem.components.snackbar.AslSnackbarHost
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.foundation.clickableWithNoFeedback
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiRole
import com.worldcup.androidstudiolite.feature.base.CollectEffects

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToAiSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBar by viewModel.snackBar.collectAsState()
    val listener: AssistantInteractionListener = viewModel
    val listState = rememberLazyListState()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            AssistantEffect.NavigateToAiSettings -> onNavigateToAiSettings()
        }
    }

    val itemCount = state.messages.size + (if (state.streaming) 1 else 0)
    LaunchedEffect(itemCount, state.streamingText.length) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Box(Modifier.fillMaxSize().background(AslTheme.colors.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AslTopBar(
                title = "AI Assistant",
                titleLeading = {
                    AslIcon(AslIcons.Sparkle, tint = AslTheme.colors.primaryContainer)
                },
                actions = {
                    Box(Modifier.clickableWithNoFeedback { listener.onAgentChipClick() }) {
                        AslStatusChip(
                            text = state.agentLabel ?: "Connect an agent",
                            status = if (state.connected) AslStatus.Running else AslStatus.Warning,
                        )
                    }
                    AslIconButton(
                        AslIcons.Delete,
                        onClick = listener::onClear,
                        contentDescription = "Clear conversation",
                    )
                },
            )

            if (!state.connected && state.messages.isEmpty()) {
                ConnectAgentHero(state, listener, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = AslTheme.spacing.gutter,
                        vertical = AslTheme.spacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
                ) {
                    itemsIndexed(state.messages) { _, message ->
                        MessageBubble(message)
                    }
                    if (state.streaming) {
                        itemsIndexed(listOf(state.streamingText)) { _, text ->
                            MessageBubble(
                                AiMessage(AiRole.Assistant, text.ifEmpty { "…" }),
                            )
                        }
                    }
                }
            }

            if (state.hasOpenFile) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AslTheme.spacing.gutter, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
                ) {
                    AslSwitch(
                        checked = state.includeOpenFile,
                        onCheckedChange = listener::onToggleIncludeOpenFile,
                    )
                    AslText(
                        "Include open file as context",
                        style = AslTheme.typography.uiLabelSmall,
                        color = AslTheme.colors.onSurfaceVariant,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AslTheme.spacing.gutter,
                        end = AslTheme.spacing.gutter,
                        bottom = AslTheme.spacing.md,
                    ),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
            ) {
                AslTextField(
                    value = state.input,
                    onValueChange = listener::onInputChange,
                    placeholder = "Ask AI to help with code…",
                    singleLine = false,
                    modifier = Modifier.weight(1f),
                )
                AslFab(
                    iconRes = AslIcons.Send,
                    onClick = listener::onSend,
                    contentDescription = "Send",
                )
            }
        }

        AslSnackbarHost(snackBar)

        ConnectAgentSheet(state, listener)
    }
}

@Composable
private fun ConnectAgentHero(
    state: AssistantUiState,
    listener: AssistantInteractionListener,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(AslTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AslIcon(AslIcons.Sparkle, size = 40.dp, tint = AslTheme.colors.primaryContainer)
        AslText("Meet your AI assistant", style = AslTheme.typography.title)
        AslText(
            "Connect an agent once and start asking about your code.",
            style = AslTheme.typography.uiBody,
            color = AslTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AslTheme.spacing.sm))
        state.providers.forEach { provider ->
            AslPrimaryButton(
                "Connect ${provider.displayName}",
                onClick = { listener.onConnectAgentClick(provider.id) },
                leadingIconRes = AslIcons.Sparkle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectAgentSheet(
    state: AssistantUiState,
    listener: AssistantInteractionListener,
) {
    val context = LocalContext.current
    val provider = state.providers.firstOrNull { it.id == state.connectProviderId }

    AslBottomSheet(
        visible = state.connectSheetVisible,
        onDismissRequest = listener::onConnectDismiss,
    ) {
        AslText(
            "Connect ${provider?.displayName ?: "an agent"}",
            style = AslTheme.typography.title,
        )
        AslText(
            "Paste an API key — the best model is picked automatically.",
            style = AslTheme.typography.uiLabelSmall,
            color = AslTheme.colors.onSurfaceVariant,
        )

        if (state.providers.size > 1) {
            Spacer(Modifier.height(AslTheme.spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
                state.providers.forEach { option ->
                    if (option.id == state.connectProviderId) {
                        AslPrimaryButton(option.displayName, onClick = {})
                    } else {
                        AslGhostButton(
                            option.displayName,
                            onClick = { listener.onConnectAgentClick(option.id) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(AslTheme.spacing.md))
        AslTextField(
            value = state.connectKeyInput,
            onValueChange = listener::onConnectKeyChange,
            placeholder = "Paste your ${provider?.displayName ?: ""} API key",
            errorText = state.connectError,
            visualTransformation = PasswordVisualTransformation(),
        )
        if (provider != null) {
            AslTextButton(
                "Get an API key",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, provider.apiKeyUrl.toUri()))
                },
            )
        }
        Spacer(Modifier.height(AslTheme.spacing.md))
        AslPrimaryButton(
            "Connect",
            onClick = listener::onConnectSubmit,
            enabled = state.connectKeyInput.isNotBlank(),
            loading = state.connecting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MessageBubble(message: AiMessage) {
    val fromUser = message.role == AiRole.User
    val colors = AslTheme.colors
    Row(Modifier.fillMaxWidth()) {
        if (fromUser) Spacer(Modifier.weight(0.15f))
        Box(
            Modifier
                .weight(0.85f)
                .background(
                    if (fromUser) colors.secondaryContainer.copy(alpha = 0.35f)
                    else colors.surfaceContainer,
                    AslTheme.shapes.default,
                )
                .padding(AslTheme.spacing.md),
        ) {
            AslText(
                message.text,
                style = if (message.text.contains("```")) AslTheme.typography.codeSmall
                else AslTheme.typography.uiBody,
            )
        }
        if (!fromUser) Spacer(Modifier.weight(0.15f))
    }
}
