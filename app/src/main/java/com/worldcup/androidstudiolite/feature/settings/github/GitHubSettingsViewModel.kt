package com.worldcup.androidstudiolite.feature.settings.github

import androidx.lifecycle.viewModelScope
import com.worldcup.androidstudiolite.domain.git.ConnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.settings.DisconnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.domain.settings.ObservePrivateReposUseCase
import com.worldcup.androidstudiolite.domain.settings.SetPrivateReposUseCase
import com.worldcup.androidstudiolite.feature.base.BaseViewModel
import kotlinx.coroutines.launch

data class GitHubSettingsUiState(
    val connected: Boolean = false,
    val owner: String = "",
    val tokenInput: String = "",
    val verifying: Boolean = false,
    val error: String? = null,
    val privateRepos: Boolean = true,
)

sealed interface GitHubSettingsEffect {
    data object Connected : GitHubSettingsEffect
}

interface GitHubSettingsInteractionListener {
    fun onTokenChange(token: String)
    fun onConnect()
    fun onDisconnect()
    fun onPrivateReposChange(enabled: Boolean)
}

class GitHubSettingsViewModel(
    private val connectGitHub: ConnectGitHubUseCase,
    private val disconnectGitHub: DisconnectGitHubUseCase,
    private val connection: ObserveGitHubConnectionUseCase,
    private val observePrivateRepos: ObservePrivateReposUseCase,
    private val setPrivateRepos: SetPrivateReposUseCase,
) : BaseViewModel<GitHubSettingsUiState, GitHubSettingsEffect>(GitHubSettingsUiState()),
    GitHubSettingsInteractionListener {

    init {
        viewModelScope.launch {
            connection.token().collect { token ->
                updateState { it.copy(connected = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            connection.owner().collect { owner ->
                updateState { it.copy(owner = owner) }
            }
        }
        viewModelScope.launch {
            observePrivateRepos().collect { enabled ->
                updateState { it.copy(privateRepos = enabled) }
            }
        }
    }

    override fun onPrivateReposChange(enabled: Boolean) {
        tryToExecute(callee = { setPrivateRepos(enabled) })
    }

    override fun onTokenChange(token: String) {
        updateState { it.copy(tokenInput = token, error = null) }
    }

    override fun onConnect() {
        val token = currentState().tokenInput
        updateState { it.copy(verifying = true, error = null) }
        tryToExecute(
            callee = { connectGitHub(token) },
            onSuccess = { account ->
                updateState {
                    it.copy(verifying = false, tokenInput = "", owner = account.login)
                }
                showSnackBar("Connected as ${account.login}")
                sendNewEffect(GitHubSettingsEffect.Connected)
            },
            onError = { error ->
                updateState { it.copy(verifying = false, error = error.message) }
            },
        )
    }

    override fun onDisconnect() {
        tryToExecute(
            callee = { disconnectGitHub() },
            onSuccess = { showSnackBar("GitHub disconnected") },
        )
    }
}
