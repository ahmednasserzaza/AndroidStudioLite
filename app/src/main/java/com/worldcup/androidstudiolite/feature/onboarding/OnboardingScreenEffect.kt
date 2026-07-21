package com.worldcup.androidstudiolite.feature.onboarding

sealed interface OnboardingScreenEffect {
    data object NavigateToGitHubSettings : OnboardingScreenEffect
    data object NavigateToProjects : OnboardingScreenEffect
}
