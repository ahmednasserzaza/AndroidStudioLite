package com.worldcup.androidstudiolite.session

import com.worldcup.androidstudiolite.entities.AiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantSession {

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun setMessages(messages: List<AiMessage>) {
        _messages.value = messages
    }

    fun clear() {
        _messages.value = emptyList()
    }
}
