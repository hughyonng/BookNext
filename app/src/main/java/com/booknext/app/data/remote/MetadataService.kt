package com.booknext.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class BookMetadata(
    val authors: List<String>,
    val publisher: String,
    val description: String,
    val coverUrl: String?,
)

class MetadataService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(title: String, apiKey: String): BookMetadata? = withContext(Dispatchers.IO) {
        try {
            val clean = cleanTitle(title)
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encoded&maxResults=5&key=$apiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext null
            if (items.length() == 0) return@withContext null

            var best: JSONObject? = null
            var bestScore = -1
            for (i in 0 until items.length()) {
                val info = items.getJSONObject(i).optJSONObject("volumeInfo") ?: continue
                val hasAuthor = info.optJSONArray("authors")?.length() ?: 0 > 0
                val t = info.optString("title", "")
                val score = when {
                    hasAuthor && t.contains(clean, ignoreCase = true) -> 3
                    hasAuthor -> 2
                    t.contains(clean, ignoreCase = true) -> 1
                    else -> 0
                }
                if (score >= 3) { best = info; break }
                if (score > bestScore) { bestScore = score; best = info }
            }
            if (best == null && items.length() > 0) {
                best = items.getJSONObject(0).optJSONObject("volumeInfo")
            }
            val info = best ?: return@withContext null

            val authors = mutableListOf<String>()
            val authorsArr = info.optJSONArray("authors")
            if (authorsArr != null) {
                for (i in 0 until authorsArr.length()) authors.add(authorsArr.getString(i))
            }

            val coverUrl = info.optJSONObject("imageLinks")?.optString("thumbnail")
                ?.replace("http://", "https://")

            BookMetadata(
                authors = authors,
                publisher = info.optString("publisher", ""),
                description = info.optString("description", ""),
                coverUrl = coverUrl,
            )
        } catch (_: Exception) { null }
    }

    suspend fun downloadCover(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext null
            val bytes = body.bytes()
            if (bytes.isEmpty()) null else bytes
        } catch (_: Exception) { null }
    }

    private fun cleanTitle(raw: String): String {
        var t = raw.trim()
        // 去掉文件扩展名
        t = t.replace(Regex("\\.[a-zA-Z0-9]{2,4}$"), "")
        // 去掉开头结尾的非中文/字母数字
        t = t.replace(Regex("^[^\\u4e00-\\u9fff\\w]+|[^\\u4e00-\\u9fff\\w]+$"), "")
        // 去掉括号内容（如 (简体) / [精校] / 第x卷）
        t = t.replace(Regex("[（(\\[].{0,20}[）)\\]]"), "")
        // 去掉常见的文件卷标
        t = t.replace(Regex("(第[一二三四五六七八九十百千万]+[卷部集册章])"), "")
        t = t.trim().take(60)
        if (t.length < 2) return raw.take(60)
        return t
    }
}
