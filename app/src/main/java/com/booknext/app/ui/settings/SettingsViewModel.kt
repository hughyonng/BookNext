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
    val eyeComfort: Boolean = false,
    val uiFontScale: Float = 1.0f,
    val uiFontFamily: String = "sans-serif",
    val uiLineSpacing: Float = 1.5f,
    val themeId: String = "blue",
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
                eyeComfort = prefs.blueLight.first(),
                uiFontScale = prefs.uiFontScale.first(),
                uiFontFamily = prefs.uiFontFamily.first(),
                uiLineSpacing = prefs.uiLineSpacing.first(),
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

    fun onUrlChange(v: String) { _state.value = _state.value.copy(serverUrl = v) }
    fun onKeyChange(v: String) { _state.value = _state.value.copy(apiKey = v) }
    fun onEyeComfortChange(v: Boolean) {
        _state.value = _state.value.copy(eyeComfort = v)
        viewModelScope.launch {
            prefs.saveBlueLight(v)
            if (v) prefs.saveBlueLightAmount(0.3f)
        }
    }
    fun onUiFontScaleChange(v: Float) {
        _state.value = _state.value.copy(uiFontScale = v)
        viewModelScope.launch { prefs.saveUiFontScale(v) }
    }
    fun onUiFontFamilyChange(v: String) {
        _state.value = _state.value.copy(uiFontFamily = v)
        viewModelScope.launch { prefs.saveUiFontFamily(v) }
    }
    fun onUiLineSpacingChange(v: Float) {
        _state.value = _state.value.copy(uiLineSpacing = v)
        viewModelScope.launch { prefs.saveUiLineSpacing(v) }
    }
    fun onThemeChange(id: String) {
        _state.value = _state.value.copy(themeId = id)
        viewModelScope.launch { prefs.saveThemeId(id) }
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            prefs.saveCredentials(s.serverUrl.trimEnd('/'), s.apiKey.trim())
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.clear()
            onDone()
        }
    }
}