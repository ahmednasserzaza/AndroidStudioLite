package com.worldcup.androidstudiolite.feature.settings.github

interface GitHubSettingsInteractionListener {
    fun onTokenChange(token: String)
    fun onConnect()
    fun onDisconnect()
    fun onPrivateReposChange(enabled: Boolean)
}
