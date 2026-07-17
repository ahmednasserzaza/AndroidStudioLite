package com.worldcup.androidstudiolite.feature.assistant

import com.worldcup.androidstudiolite.entities.AiMessage
import com.worldcup.androidstudiolite.entities.AiProviderDescriptor

data class AssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val streamingText: String = "",
    val agentLabel: String? = null,
    val includeOpenFile: Boolean = true,
    val hasOpenFile: Boolean = false,
    val connected: Boolean = false,
    val providers: List<AiProviderDescriptor> = emptyList(),
    val connectSheetVisible: Boolean = false,
    val connectProviderId: String? = null,
    val connectKeyInput: String = "",
    val connecting: Boolean = false,
    val connectError: String? = null,
)

sealed interface AssistantEffect {
    data object NavigateToAiSettings : AssistantEffect
}

interface AssistantInteractionListener {
    fun onInputChange(text: String)
    fun onToggleIncludeOpenFile(include: Boolean)
    fun onSend()
    fun onClear()
    fun onAgentChipClick()
    fun onConnectAgentClick(providerId: String)
    fun onConnectKeyChange(key: String)
    fun onConnectSubmit()
    fun onConnectDismiss()
}
