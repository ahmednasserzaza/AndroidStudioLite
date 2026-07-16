package com.worldcup.androidstudiolite.di

import com.worldcup.androidstudiolite.domain.ai.ConnectAiProviderUseCase
import com.worldcup.androidstudiolite.domain.ai.GetAiModelsUseCase
import com.worldcup.androidstudiolite.domain.ai.GetAiProvidersUseCase
import com.worldcup.androidstudiolite.domain.ai.ObserveAgentConfigUseCase
import com.worldcup.androidstudiolite.domain.ai.SelectAiModelUseCase
import com.worldcup.androidstudiolite.domain.ai.StreamAssistantReplyUseCase
import com.worldcup.androidstudiolite.domain.build.RunBuildUseCase
import com.worldcup.androidstudiolite.domain.files.CreateFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.DeleteFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.GetFileTreeUseCase
import com.worldcup.androidstudiolite.domain.files.ReadFileUseCase
import com.worldcup.androidstudiolite.domain.files.RenameFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.git.CommitAndPushUseCase
import com.worldcup.androidstudiolite.domain.git.ConnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.git.EnsureOwnerUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitsUseCase
import com.worldcup.androidstudiolite.domain.git.PullProjectUseCase
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.domain.project.DeleteProjectUseCase
import com.worldcup.androidstudiolite.domain.project.GetProjectsUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.domain.settings.CompleteOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.DisconnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.ObservePrivateReposUseCase
import com.worldcup.androidstudiolite.domain.settings.SetPrivateReposUseCase
import com.worldcup.androidstudiolite.feature.assistant.AssistantViewModel
import com.worldcup.androidstudiolite.feature.editor.EditorViewModel
import com.worldcup.androidstudiolite.feature.onboarding.OnboardingViewModel
import com.worldcup.androidstudiolite.feature.projects.ProjectsViewModel
import com.worldcup.androidstudiolite.feature.settings.SettingsViewModel
import com.worldcup.androidstudiolite.feature.settings.ai.AiSettingsViewModel
import com.worldcup.androidstudiolite.feature.settings.github.GitHubSettingsViewModel
import com.worldcup.androidstudiolite.feature.vcs.VcsViewModel
import com.worldcup.androidstudiolite.session.AssistantSession
import com.worldcup.androidstudiolite.session.BuildSession
import com.worldcup.androidstudiolite.session.WorkspaceSession
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sessionModule = module {
    single { WorkspaceSession() }
    single { AssistantSession() }
    single { BuildSession(context = androidContext(), runBuild = get()) }
}

val useCaseModule = module {
    factoryOf(::GetProjectsUseCase)
    factoryOf(::CreateProjectUseCase)
    factoryOf(::DeleteProjectUseCase)
    factoryOf(::RepairProjectInfrastructureUseCase)
    factoryOf(::GetFileTreeUseCase)
    factoryOf(::ReadFileUseCase)
    factoryOf(::SaveFileUseCase)
    factoryOf(::CreateFileEntryUseCase)
    factoryOf(::RenameFileEntryUseCase)
    factoryOf(::DeleteFileEntryUseCase)
    factoryOf(::ConnectGitHubUseCase)
    factoryOf(::EnsureOwnerUseCase)
    factoryOf(::GetCommitsUseCase)
    factoryOf(::CommitAndPushUseCase)
    factoryOf(::PullProjectUseCase)
    factoryOf(::RunBuildUseCase)
    factoryOf(::GetAiProvidersUseCase)
    factoryOf(::ConnectAiProviderUseCase)
    factoryOf(::GetAiModelsUseCase)
    factoryOf(::SelectAiModelUseCase)
    factoryOf(::ObserveAgentConfigUseCase)
    factoryOf(::StreamAssistantReplyUseCase)
    factoryOf(::ObserveGitHubConnectionUseCase)
    factoryOf(::DisconnectGitHubUseCase)
    factoryOf(::ObserveOnboardingUseCase)
    factoryOf(::ObservePrivateReposUseCase)
    factoryOf(::SetPrivateReposUseCase)
    factoryOf(::CompleteOnboardingUseCase)
}

val appModule = module {
    viewModelOf(::ProjectsViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::VcsViewModel)
    viewModelOf(::AssistantViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::GitHubSettingsViewModel)
    viewModelOf(::AiSettingsViewModel)
    viewModelOf(::OnboardingViewModel)
}
