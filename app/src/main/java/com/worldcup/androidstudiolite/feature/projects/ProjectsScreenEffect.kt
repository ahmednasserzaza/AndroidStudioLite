package com.worldcup.androidstudiolite.feature.projects

sealed interface ProjectsScreenEffect {
    data object NavigateToEditor : ProjectsScreenEffect
    data object NavigateToGitHubSettings : ProjectsScreenEffect
}
