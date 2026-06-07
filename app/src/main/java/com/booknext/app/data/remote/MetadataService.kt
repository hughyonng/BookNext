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
            // 清理标题：去掉文件扩展名和截断
            val cleanTitle = title
                .replace(Regex("\\.(epub|pdf|txt|mobi|azw3)$", RegexOption.IGNORE_CASE), "")
                .take(60)
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encoded&maxResults=3&key=$apiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext null
            if (items.length() == 0) return@withContext null

            // 取第一个有作者的结果
            var best: JSONObject? = null
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val info = item.optJSONObject("volumeInfo")
                if (info != null && info.optJSONArray("authors") != null && info.optJSONArray("authors")!!.length() > 0) {
                    best = info
                    break
                }
            }
            if (best == null) {
                best = items.getJSONObject(0).optJSONObject("volumeInfo")
                if (best == null) return@withContext null
            }
            val info = best!!

            val authors = mutableListOf<String>()
            val authorsArr = info.optJSONArray("authors")
            if (authorsArr != null) {
                for (i in 0 until authorsArr.length()) authors.add(authorsArr.getString(i))
            }

            val publisher = info.optString("publisher", "")
            val description = info.optString("description", "")
            val coverUrl = info.optJSONObject("imageLinks")?.optString("thumbnail")
                ?.replace("http://", "https://")

            BookMetadata(
                authors = authors,
                publisher = publisher,
                description = description,
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
}
