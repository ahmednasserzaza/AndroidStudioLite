package com.worldcup.androidstudiolite.feature.settings.github

sealed interface GitHubSettingsScreenEffect {
    data object Connected : GitHubSettingsScreenEffect
}
