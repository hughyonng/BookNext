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
            val queries = buildQueries(title)
            for (q in queries) {
                val meta = tryQuery(q, apiKey)
                if (meta != null) return@withContext meta
            }
            null
        } catch (_: Exception) { null }
    }

    private suspend fun tryQuery(queryTitle: String, apiKey: String): BookMetadata? {
        try {
            val encoded = URLEncoder.encode(queryTitle, "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encoded&maxResults=5&key=$apiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return null
            if (items.length() == 0) return null

            // 评分：标题含完整查询词 + 有作者 >= 不含查询词
            var best: JSONObject? = null
            var bestScore = -1
            for (i in 0 until items.length()) {
                val info = items.getJSONObject(i).optJSONObject("volumeInfo") ?: continue
                val hasAuthor = info.optJSONArray("authors")?.length() ?: 0 > 0
                val t = info.optString("title", "")
                val match = t.contains(queryTitle, ignoreCase = true)
                val score = when {
                    hasAuthor && match -> 3
                    hasAuthor -> 2
                    match -> 1
                    else -> 0
                }
                if (score >= 3) { best = info; break }
                if (score > bestScore) { bestScore = score; best = info }
            }
            if (best == null) best = items.getJSONObject(0).optJSONObject("volumeInfo")
            val info = best ?: return null

            val authors = mutableListOf<String>()
            val authorsArr = info.optJSONArray("authors")
            if (authorsArr != null) {
                for (i in 0 until authorsArr.length()) authors.add(authorsArr.getString(i))
            }

            val coverUrl = info.optJSONObject("imageLinks")?.optString("thumbnail")
                ?.replace("http://", "https://")

            return BookMetadata(
                authors = authors,
                publisher = info.optString("publisher", ""),
                description = info.optString("description", ""),
                coverUrl = coverUrl,
            )
        } catch (_: Exception) { return null }
    }

    private fun buildQueries(raw: String): List<String> {
        var t = raw.trim()
        val queries = mutableListOf<String>()
        // 原书名直接查
        queries.add(takeSensible(t))
        // 去掉文件扩展名
        t = t.replace(Regex("\\.[a-zA-Z0-9]{2,4}$"), "")
        queries.add(takeSensible(t))
        // 去掉方括号内容 [xxx] （常见于精校版/排版等标记）
        val noBracket = t.replace(Regex("\\[.*?]"), "").trim()
        if (noBracket != t && noBracket.length >= 2) {
            queries.add(takeSensible(noBracket))
            // 去掉中文括号 （xxx）
            val noParen = noBracket.replace(Regex("[（(].{0,30}[）)]"), "").trim()
            if (noParen != noBracket && noParen.length >= 2) queries.add(takeSensible(noParen))
        }
        // 取书名前 2-4 个字（最后的 fallback）
        val short = t.take(4).trim()
        if (short.length >= 2 && short != t) queries.add(short)
        return queries.distinct()
    }

    private fun takeSensible(s: String) = s.trim().take(60).ifEmpty { s.take(60) }

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
