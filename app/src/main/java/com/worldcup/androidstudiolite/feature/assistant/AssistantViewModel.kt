package com.worldcup.androidstudiolite.feature.assistant

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.GetAiProvidersUseCase
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.ai.StreamAssistantReplyUseCase
import com.worldcup.androidstudiolite.domain.exception.DomainException
import com.worldcup.androidstudiolite.entities.AiFileContext
import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiRole
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import com.worldcup.androidstudiolite.session.AssistantSession
import com.worldcup.androidstudiolite.session.WorkspaceSession
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val streamReply: StreamAssistantReplyUseCase,
    private val observeAgentConfig: ObserveAgentConfigUseCase,
    private val getProviders: GetAiProvidersUseCase,
    private val session: AssistantSession,
    private val workspace: WorkspaceSession,
) : BaseViewModel<AssistantUiState, AssistantEffect>(AssistantUiState()),
    AssistantInteractionListener {

    init {
        updateState { it.copy(messages = session.messages.value) }
        viewModelScope.launch {
            observeAgentConfig().collect { config ->
                val label = config?.let { c ->
                    val provider = getProviders().firstOrNull { it.id == c.providerId }
                    "${provider?.displayName ?: c.providerId} · ${c.modelId}"
                }
                updateState { it.copy(connected = config != null, agentLabel = label) }
            }
        }
        viewModelScope.launch {
            workspace.activeFilePath.collect { path ->
                updateState { it.copy(hasOpenFile = path != null) }
            }
        }
    }

    override fun onInputChange(text: String) {
        updateState { it.copy(input = text) }
    }

    override fun onToggleIncludeOpenFile(include: Boolean) {
        updateState { it.copy(includeOpenFile = include) }
    }

    override fun onSend() {
        val text = currentState().input.trim()
        if (text.isEmpty() || currentState().streaming) return
        if (!currentState().connected) {
            sendNewEffect(AssistantEffect.NavigateToAiSettings)
            return
        }

        val history = currentState().messages + AiMessage(AiRole.User, text)
        session.setMessages(history)
        updateState { it.copy(messages = history, input = "", streaming = true, streamingText = "") }

        val fileContext = if (currentState().includeOpenFile) openFileContext() else null

        tryToCollect(
            callee = { streamReply(history, fileContext) },
            onNewValue = { chunk ->
                updateState { it.copy(streamingText = it.streamingText + chunk.text) }
            },
            onCompletion = {
                val reply = currentState().streamingText.ifBlank { "(no response)" }
                val updated = currentState().messages + AiMessage(AiRole.Assistant, reply)
                session.setMessages(updated)
                updateState { it.copy(messages = updated, streaming = false, streamingText = "") }
            },
            onError = { error ->
                updateState { it.copy(streaming = false, streamingText = "") }
                if (error is DomainException.Auth || error is DomainException.Validation) {
                    sendNewEffect(AssistantEffect.NavigateToAiSettings)
                }
            },
        )
    }

    private fun openFileContext(): AiFileContext? {
        val active = workspace.openFiles.value
            .firstOrNull { it.path == workspace.activeFilePath.value } ?: return null
        return AiFileContext(path = active.relativePath, content = active.content)
    }

    override fun onClear() {
        session.clear()
        updateState { it.copy(messages = emptyList(), streamingText = "") }
    }

    override fun onAgentChipClick() = sendNewEffect(AssistantEffect.NavigateToAiSettings)
}
