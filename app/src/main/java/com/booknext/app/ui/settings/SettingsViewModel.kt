package com.booknext.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val darkMode: Boolean = false,
    val fontSize: Int = 17,
    val fontFamily: String = "serif",
    val lineSpacing: Float = 1.8f,
    val customFontName: String = "",
    val themeId: String = "blue",
    val saved: Boolean = false,
    val cacheSize: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        viewModelScope.launch {
            _state.value = SettingsState(
                serverUrl = prefs.serverUrl.first(),
                apiKey = prefs.apiKey.first(),
                darkMode = prefs.darkMode.first(),
                fontSize = prefs.fontSize.first(),
                fontFamily = prefs.fontFamily.first(),
                lineSpacing = prefs.lineSpacing.first(),
                customFontName = run {
                    val path = prefs.customFontPath.first()
                    if (path.isEmpty()) "" else path.substringAfterLast("/").substringAfterLast("%2F")
                },
                themeId = prefs.themeId.first(),
            )
            refreshCacheSize()
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "books")
            val size = if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
            val display = when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${"%,d".format(size / (1024 * 1024))} MB"
            }
            _state.value = _state.value.copy(cacheSize = display)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "books")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            File(context.cacheDir, "tts_tmp.mp3").delete()
            refreshCacheSize()
        }
    }

    fun onUrlChange(v: String) { _state.value = _state.value.copy(serverUrl = v, saved = false) }
    fun onKeyChange(v: String) { _state.value = _state.value.copy(apiKey = v, saved = false) }
    fun onDarkModeChange(v: Boolean) { _state.value = _state.value.copy(darkMode = v, saved = false) }
    fun onFontSizeChange(v: Int) { _state.value = _state.value.copy(fontSize = v, saved = false) }
    fun onFontFamilyChange(v: String) { _state.value = _state.value.copy(fontFamily = v, saved = false) }
    fun onLineSpacingChange(v: Float) { _state.value = _state.value.copy(lineSpacing = v, saved = false) }
    fun onThemeChange(id: String) {
        _state.value = _state.value.copy(themeId = id, saved = false)
        viewModelScope.launch { prefs.saveThemeId(id) }
    }
    fun onCustomFontPicked(uri: android.net.Uri) {
        viewModelScope.launch {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val path = uri.toString()
            prefs.saveCustomFontPath(path)
            prefs.saveFontFamily("custom")
            val name = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast("%2F")
                ?: "自定义字体"
            _state.value = _state.value.copy(
                fontFamily = "custom",
                customFontName = name,
                saved = false,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            prefs.saveCredentials(s.serverUrl.trimEnd('/'), s.apiKey.trim())
            prefs.saveDarkMode(s.darkMode)
            prefs.saveFontSize(s.fontSize)
            prefs.saveFontFamily(s.fontFamily)
            prefs.saveLineSpacing(s.lineSpacing)
            prefs.saveThemeId(s.themeId)
            _state.value = s.copy(saved = true)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.clear()
            onDone()
        }
    }
}