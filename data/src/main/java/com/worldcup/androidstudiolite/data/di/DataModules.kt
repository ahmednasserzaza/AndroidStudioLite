package com.worldcup.androidstudiolite.data.di

import android.util.Log
import com.worldcup.androidstudiolite.data.local.fs.ProjectFileSystemDataSource
import com.worldcup.androidstudiolite.data.local.prefs.SettingsDataSource
import com.worldcup.androidstudiolite.data.remote.github.GitHubDataSource
import com.worldcup.androidstudiolite.data.repository.GitHubRepositoryImpl
import com.worldcup.androidstudiolite.data.repository.ProjectFilesRepositoryImpl
import com.worldcup.androidstudiolite.data.repository.ProjectRepositoryImpl
import com.worldcup.androidstudiolite.data.repository.SettingsRepositoryImpl
import com.worldcup.androidstudiolite.domain.repository.GitHubRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectFilesRepository
import com.worldcup.androidstudiolite.domain.repository.ProjectRepository
import com.worldcup.androidstudiolite.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            followRedirects = false
            engine {
                config { followRedirects(true) }
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 20_000L
                requestTimeoutMillis = 20 * 60_000L
                socketTimeoutMillis = 120_000L
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.i("API", message)
                    }
                }
                level = LogLevel.INFO
            }
        }
    }

    single { GitHubDataSource(client = get()) { get<SettingsDataSource>().githubToken } }
}

val localModule = module {
    single { ProjectFileSystemDataSource(androidContext()) }
    single { SettingsDataSource(androidContext()) }
}

val repositoryModule = module {
    single<ProjectRepository> { ProjectRepositoryImpl(fs = get()) }
    single<ProjectFilesRepository> { ProjectFilesRepositoryImpl(fs = get()) }
    single<GitHubRepository> { GitHubRepositoryImpl(github = get(), fs = get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(local = get()) }
}

val dataModules = listOf(networkModule, localModule, repositoryModule)
