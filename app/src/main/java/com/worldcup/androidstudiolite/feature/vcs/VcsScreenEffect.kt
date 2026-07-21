package com.worldcup.androidstudiolite.feature.vcs

sealed interface VcsScreenEffect {
    data object NavigateToProjects : VcsScreenEffect
    data object NavigateToEditor : VcsScreenEffect
}
