package com.booknext.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.api.BookNextApi
import com.booknext.app.data.remote.dto.AuthRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

data class LoginState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val loading: Boolean = false,
    val error: String = "",
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val client = OkHttpClient()

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun onUrlChange(v: String) { _state.value = _state.value.copy(serverUrl = v) }
    fun onKeyChange(v: String) { _state.value = _state.value.copy(apiKey = v) }

    fun login(onSuccess: () -> Unit) {
        val url = _state.value.serverUrl.trimEnd('/')
        val key = _state.value.apiKey.trim()
        if (url.isEmpty() || key.isEmpty()) {
            _state.value = _state.value.copy(error = "请填写服务器地址和密钥")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = "")
            try {
                val api = Retrofit.Builder()
                    .baseUrl("${url}/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BookNextApi::class.java)
                val resp = api.auth(AuthRequest(key))
                if (resp.valid) {
                    prefs.saveCredentials(url, key)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(error = "密钥无效，请检查后重试")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "登录失败：${e.message}")
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
