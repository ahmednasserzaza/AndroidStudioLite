package com.worldcup.androidstudiolite.feature.assistant

import com.worldcup.androidstudiolite.entities.AiMessage

data class AssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val streamingText: String = "",
    val agentLabel: String? = null,
    val includeOpenFile: Boolean = true,
    val hasOpenFile: Boolean = false,
    val connected: Boolean = false,
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
}
