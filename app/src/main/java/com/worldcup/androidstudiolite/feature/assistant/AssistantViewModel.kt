package com.worldcup.androidstudiolite.feature.assistant

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.ai.ConnectAiProviderUseCase
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
    private val connectProvider: ConnectAiProviderUseCase,
    private val session: AssistantSession,
    private val workspace: WorkspaceSession,
) : BaseViewModel<AssistantUiState, AssistantEffect>(AssistantUiState()),
    AssistantInteractionListener {

    init {
        val providers = getProviders()
        updateState {
            it.copy(
                messages = session.messages.value,
                providers = providers,
                connectProviderId = providers.firstOrNull()?.id,
            )
        }
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
            openConnectSheet()
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
                    openConnectSheet()
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

    override fun onAgentChipClick() {
        if (currentState().connected) {
            sendNewEffect(AssistantEffect.NavigateToAiSettings)
        } else {
            openConnectSheet()
        }
    }

    override fun onConnectAgentClick(providerId: String) {
        updateState {
            it.copy(connectSheetVisible = true, connectProviderId = providerId, connectError = null)
        }
    }

    override fun onConnectKeyChange(key: String) {
        updateState { it.copy(connectKeyInput = key, connectError = null) }
    }

    override fun onConnectSubmit() {
        val providerId = currentState().connectProviderId ?: return
        val key = currentState().connectKeyInput
        if (key.isBlank() || currentState().connecting) return
        updateState { it.copy(connecting = true, connectError = null) }
        tryToExecute(
            callee = { connectProvider(providerId, key) },
            onSuccess = {
                updateState {
                    it.copy(connecting = false, connectSheetVisible = false, connectKeyInput = "")
                }
                showSnackBar("Agent connected — ask away")
            },
            onError = { error ->
                updateState {
                    it.copy(
                        connecting = false,
                        connectError = error.message ?: "This key was rejected",
                    )
                }
            },
        )
    }

    override fun onConnectDismiss() {
        if (currentState().connecting) return
        updateState { it.copy(connectSheetVisible = false) }
    }

    private fun openConnectSheet() {
        val providerId = currentState().connectProviderId
            ?: currentState().providers.firstOrNull()?.id
            ?: return
        onConnectAgentClick(providerId)
    }
}
