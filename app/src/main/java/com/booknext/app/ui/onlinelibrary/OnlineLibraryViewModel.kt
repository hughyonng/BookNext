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

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val file = File(context.filesDir, "online_sources.json")
            _sources.value = if (file.exists()) {
                val text = file.readText()
                val arr = JSONArray(text)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    OnlineSource(
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        desc = obj.getString("desc"),
                    )
                }
            } else {
                defaultSources().also { saveToFile(it) }
            }
        }
    }

    private fun defaultSources() = listOf(
        OnlineSource("Anna's Archive", "https://annas-archive.gl/", "全球最大的影子图书馆聚合搜索引擎"),
        OnlineSource("FMHY Reading", "https://fmhy.net/reading", "Free Media Hell Yeah 阅读资源合集"),
        OnlineSource("Z-Lib", "https://z-lib.fm/", "全球最大的数字图书馆之一"),
        OnlineSource("MyComic", "https://mycomic.com/", "漫画在线阅读平台"),
        OnlineSource("Project Gutenberg", "https://www.gutenberg.org", "超过7万本免费电子书，版权过期经典著作"),
        OnlineSource("Standard Ebooks", "https://standardebooks.org", "精校版免费电子书，注重排版质量"),
        OnlineSource("古登堡计划中文站", "https://www.gutenberg.org/browse/languages/zh", "Project Gutenberg 中文书籍分类"),
        OnlineSource("ManyBooks", "https://manybooks.net", "超过5万本免费电子书，多格式下载"),
        OnlineSource("Open Library", "https://openlibrary.org", "互联网档案馆的在线图书馆项目"),
    )

    private fun saveToFile(list: List<OnlineSource>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("name", s.name); put("url", s.url); put("desc", s.desc)
            })
        }
        File(context.filesDir, "online_sources.json").writeText(arr.toString(2))
    }

    fun addSource(name: String, url: String, desc: String) {
        val updated = _sources.value + OnlineSource(name, url, desc)
        _sources.value = updated
        saveToFile(updated)
    }

    fun deleteSource(index: Int) {
        val updated = _sources.value.toMutableList().also { it.removeAt(index) }
        _sources.value = updated
        saveToFile(updated)
    }
}
