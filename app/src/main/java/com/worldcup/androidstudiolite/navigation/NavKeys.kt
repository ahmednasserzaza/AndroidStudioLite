package com.worldcup.androidstudiolite.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object ProjectsKey : NavKey
@Serializable data object EditorKey : NavKey
@Serializable data object VcsKey : NavKey

@Serializable data object OnboardingKey : NavKey
@Serializable data object GitHubSettingsKey : NavKey
@Serializable data object BuildProgressKey : NavKey

val TOP_LEVEL_KEYS: List<NavKey> =
    listOf(ProjectsKey, EditorKey, VcsKey)
