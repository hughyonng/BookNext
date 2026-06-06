package com.booknext.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TranslationResult(
    val success: Boolean,
    val text: String = "",
    val sourceLang: String = "",
    val error: String = "",
)

@Singleton
class TranslatorService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, targetLang: String = "zh-CN"): TranslationResult {
        if (text.isBlank()) return TranslationResult(false, error = "没有文字可翻译")
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
                val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext TranslationResult(false, error = "空响应")

                val arr = JSONArray(body)
                val sentences = arr.getJSONArray(0)
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val segment = sentences.getJSONArray(i)
                    sb.append(segment.optString(0, ""))
                }
                val sourceLang = arr.optString(2, "auto")
                TranslationResult(
                    success = sb.isNotEmpty(),
                    text = sb.toString(),
                    sourceLang = sourceLang,
                )
            } catch (e: Exception) {
                TranslationResult(false, error = e.message ?: "翻译失败")
            }
        }
    }
}
