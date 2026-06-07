package com.booknext.app.ui.onlinelibrary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class OnlineSource(
    val name: String,
    val url: String,
    val desc: String,
)

@HiltViewModel
class OnlineLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _sources = MutableStateFlow<List<OnlineSource>>(emptyList())
    val sources: StateFlow<List<OnlineSource>> = _sources

    val fixedCount: Int get() = fixedSources().size

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val fixed = fixedSources()
            val file = File(context.filesDir, "online_sources.json")
            val user = if (file.exists()) {
                val text = file.readText()
                val arr = JSONArray(text)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    OnlineSource(obj.getString("name"), obj.getString("url"), obj.getString("desc"))
                }
            } else { emptyList() }
            _sources.value = fixed + user
        }
    }

    private fun fixedSources() = listOf(
        OnlineSource("Anna's Archive", "https://annas-archive.gl/", "全球最大的影子图书馆聚合搜索引擎"),
        OnlineSource("FMHY Reading", "https://fmhy.net/reading", "Free Media Hell Yeah 阅读资源合集"),
        OnlineSource("Z-Lib", "https://z-lib.fm/", "全球最大的数字图书馆之一"),
        OnlineSource("MyComic", "https://mycomic.com/", "漫画在线阅读平台"),
    )

    private fun saveUserSources() {
        val fixed = fixedSources().size
        val user = _sources.value.drop(fixed)
        val arr = JSONArray()
        user.forEach { s ->
            arr.put(JSONObject().apply {
                put("name", s.name); put("url", s.url); put("desc", s.desc)
            })
        }
        File(context.filesDir, "online_sources.json").writeText(arr.toString(2))
    }

    fun addSource(name: String, url: String, desc: String) {
        _sources.value = _sources.value + OnlineSource(name, url, desc)
        saveUserSources()
    }

    fun deleteSource(index: Int) {
        val fixed = fixedSources().size
        if (index < fixed) return // 固定源不可删除
        val updated = _sources.value.toMutableList().also { it.removeAt(index) }
        _sources.value = updated
        saveUserSources()
    }
}
