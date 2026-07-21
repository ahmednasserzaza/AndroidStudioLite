package com.worldcup.androidstudiolite.feature.editor

sealed interface EditorScreenEffect {
    data object NavigateToBuild : EditorScreenEffect
    data object NavigateToProjects : EditorScreenEffect
}
