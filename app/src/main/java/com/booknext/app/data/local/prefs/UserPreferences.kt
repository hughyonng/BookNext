package com.booknext.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "booknext_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val API_KEY    = stringPreferencesKey("api_key")
        private val DARK_MODE  = booleanPreferencesKey("dark_mode")
        private val FONT_SIZE  = intPreferencesKey("font_size")
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        private val THEME_ID = stringPreferencesKey("theme_id")
        private val EMPTY_FOLDERS = stringSetPreferencesKey("empty_folders")
        private val HAS_SEEN_WELCOME_KEY = booleanPreferencesKey("has_seen_welcome")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "" }
    val apiKey: Flow<String>    = context.dataStore.data.map { it[API_KEY] ?: "" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val fontSize: Flow<Int>     = context.dataStore.data.map { it[FONT_SIZE] ?: 17 }
    val fontFamily: Flow<String> = context.dataStore.data.map { it[FONT_FAMILY] ?: "serif" }
    val lineSpacing: Flow<Float> = context.dataStore.data.map { it[LINE_SPACING] ?: 1.8f }
    val customFontPath: Flow<String> = context.dataStore.data.map { it[CUSTOM_FONT_PATH] ?: "" }
    val themeId: Flow<String> = context.dataStore.data.map { it[THEME_ID] ?: "blue" }

    suspend fun saveCredentials(url: String, key: String) {
        context.dataStore.edit {
            it[SERVER_URL] = url.trimEnd('/')
            it[API_KEY]    = key
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveFontSize(size: Int) {
        context.dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun saveFontFamily(family: String) {
        context.dataStore.edit { it[FONT_FAMILY] = family }
    }

    suspend fun saveLineSpacing(spacing: Float) {
        context.dataStore.edit { it[LINE_SPACING] = spacing }
    }

    suspend fun saveCustomFontPath(path: String) {
        context.dataStore.edit { it[CUSTOM_FONT_PATH] = path }
    }

    suspend fun saveThemeId(id: String) {
        context.dataStore.edit { it[THEME_ID] = id }
    }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map {
        it[HAS_SEEN_WELCOME_KEY] ?: false
    }

    suspend fun setHasSeenWelcome(value: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_WELCOME_KEY] = value }
    }

    val emptyFolders: Flow<Set<String>> = context.dataStore.data.map {
        it[EMPTY_FOLDERS] ?: emptySet()
    }

    suspend fun addEmptyFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current: Set<String> = prefs[EMPTY_FOLDERS] ?: emptySet()
            prefs[EMPTY_FOLDERS] = current + name
        }
    }

    suspend fun removeEmptyFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current: Set<String> = prefs[EMPTY_FOLDERS] ?: emptySet()
            prefs[EMPTY_FOLDERS] = current - name
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
