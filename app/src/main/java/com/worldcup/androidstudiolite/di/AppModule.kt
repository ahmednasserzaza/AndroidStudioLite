package com.worldcup.androidstudiolite.di

import com.worldcup.androidstudiolite.domain.build.RunBuildUseCase
import com.worldcup.androidstudiolite.domain.files.CreateFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.DeleteFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.GetFileTreeUseCase
import com.worldcup.androidstudiolite.domain.files.ReadFileUseCase
import com.worldcup.androidstudiolite.domain.files.RenameFileEntryUseCase
import com.worldcup.androidstudiolite.domain.files.SaveFileUseCase
import com.worldcup.androidstudiolite.domain.files.SearchProjectUseCase
import com.worldcup.androidstudiolite.domain.git.CheckoutBranchUseCase
import com.worldcup.androidstudiolite.domain.git.CommitAndPushUseCase
import com.worldcup.androidstudiolite.domain.git.ConnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.git.CreateBranchUseCase
import com.worldcup.androidstudiolite.domain.git.CreatePullRequestUseCase
import com.worldcup.androidstudiolite.domain.git.DeleteBranchUseCase
import com.worldcup.androidstudiolite.domain.git.DiscardFileChangeUseCase
import com.worldcup.androidstudiolite.domain.git.EnsureOwnerUseCase
import com.worldcup.androidstudiolite.domain.git.GetBranchChecksUseCase
import com.worldcup.androidstudiolite.domain.git.GetBranchStatusUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitDetailUseCase
import com.worldcup.androidstudiolite.domain.git.GetCommitsUseCase
import com.worldcup.androidstudiolite.domain.git.GetLocalChangesUseCase
import com.worldcup.androidstudiolite.domain.git.ListBranchesUseCase
import com.worldcup.androidstudiolite.domain.git.ListPullRequestsUseCase
import com.worldcup.androidstudiolite.domain.git.MergePullRequestUseCase
import com.worldcup.androidstudiolite.domain.git.PullProjectUseCase
import com.worldcup.androidstudiolite.domain.project.CreateProjectUseCase
import com.worldcup.androidstudiolite.domain.project.DeleteProjectUseCase
import com.worldcup.androidstudiolite.domain.project.GetLastProjectUseCase
import com.worldcup.androidstudiolite.domain.project.GetProjectsUseCase
import com.worldcup.androidstudiolite.domain.project.ImportRepoUseCase
import com.worldcup.androidstudiolite.domain.project.ListImportableReposUseCase
import com.worldcup.androidstudiolite.domain.project.RepairProjectInfrastructureUseCase
import com.worldcup.androidstudiolite.domain.project.SaveLastProjectUseCase
import com.worldcup.androidstudiolite.domain.settings.CompleteOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.DisconnectGitHubUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveGitHubConnectionUseCase
import com.worldcup.androidstudiolite.domain.settings.ObserveOnboardingUseCase
import com.worldcup.androidstudiolite.domain.settings.ObservePrivateReposUseCase
import com.worldcup.androidstudiolite.domain.settings.SetPrivateReposUseCase
import com.worldcup.androidstudiolite.feature.editor.EditorViewModel
import com.worldcup.androidstudiolite.feature.onboarding.OnboardingViewModel
import com.worldcup.androidstudiolite.feature.projects.ProjectsViewModel
import com.worldcup.androidstudiolite.feature.settings.github.GitHubSettingsViewModel
import com.worldcup.androidstudiolite.feature.vcs.VcsViewModel
import com.worldcup.androidstudiolite.session.BuildSession
import com.worldcup.androidstudiolite.session.WorkspaceSession
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sessionModule = module {
    single { WorkspaceSession() }
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
    factoryOf(::SearchProjectUseCase)
    factoryOf(::ListImportableReposUseCase)
    factoryOf(::ImportRepoUseCase)
    factoryOf(::SaveLastProjectUseCase)
    factoryOf(::GetLastProjectUseCase)
    factoryOf(::ConnectGitHubUseCase)
    factoryOf(::EnsureOwnerUseCase)
    factoryOf(::GetCommitsUseCase)
    factoryOf(::CommitAndPushUseCase)
    factoryOf(::PullProjectUseCase)
    factoryOf(::ListBranchesUseCase)
    factoryOf(::CreateBranchUseCase)
    factoryOf(::CheckoutBranchUseCase)
    factoryOf(::DeleteBranchUseCase)
    factoryOf(::GetLocalChangesUseCase)
    factoryOf(::GetBranchStatusUseCase)
    factoryOf(::GetCommitDetailUseCase)
    factoryOf(::GetBranchChecksUseCase)
    factoryOf(::ListPullRequestsUseCase)
    factoryOf(::CreatePullRequestUseCase)
    factoryOf(::MergePullRequestUseCase)
    factoryOf(::DiscardFileChangeUseCase)
    factoryOf(::RunBuildUseCase)
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
    viewModelOf(::GitHubSettingsViewModel)
    viewModelOf(::OnboardingViewModel)
}
