package com.worldcup.androidstudiolite.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "asl_settings_store")

class SettingsDataSource(private val context: Context) {

    private val store = context.settingsStore

    private val secure by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "asl_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also { migrateLegacyPrefs(it) }
    }

    private fun migrateLegacyPrefs(securePrefs: android.content.SharedPreferences) {
        val legacy = context.getSharedPreferences("asl_settings", Context.MODE_PRIVATE)
        if (legacy.getBoolean("migrated_to_secure", false)) return
        val editor = securePrefs.edit()
        legacy.getString("github_token", null)?.takeIf { it.isNotBlank() }?.let {
            editor.putString(KEY_GITHUB_TOKEN, it)
        }
        editor.apply()
        legacy.edit().putBoolean("migrated_to_secure", true).apply()
    }


    var githubToken: String
        get() = secure.getString(KEY_GITHUB_TOKEN, "") ?: ""
        set(value) {
            secure.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()
        }

    val tokenVersion: Flow<Int> = store.data.map { it[TOKEN_VERSION] ?: 0 }

    suspend fun bumpTokenVersion() {
        store.edit { it[TOKEN_VERSION] = ((it[TOKEN_VERSION] ?: 0) + 1) }
    }

    val githubOwner: Flow<String> = store.data.map { prefs ->
        prefs[GITHUB_OWNER] ?: legacyString("github_owner")
    }

    suspend fun setGithubOwner(owner: String) {
        store.edit { it[GITHUB_OWNER] = owner.trim() }
    }

    val privateRepos: Flow<Boolean> = store.data.map { it[PRIVATE_REPOS] ?: true }

    suspend fun setPrivateRepos(enabled: Boolean) {
        store.edit { it[PRIVATE_REPOS] = enabled }
    }

    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[ONBOARDING_DONE] = done }
    }

    private fun legacyString(key: String): String =
        context.getSharedPreferences("asl_settings", Context.MODE_PRIVATE)
            .getString(key, "") ?: ""

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_token"

        private val TOKEN_VERSION = androidx.datastore.preferences.core.intPreferencesKey("token_version")
        private val GITHUB_OWNER = stringPreferencesKey("github_owner")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val PRIVATE_REPOS = booleanPreferencesKey("private_repos")
    }
}
