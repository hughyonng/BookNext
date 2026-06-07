package com.booknext.app.ui.cloud

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class CloudFolder(
    val name: String,
    val displayName: String,
    val books: List<BookEntity>,
)

sealed class CloudUiState {
    object Loading : CloudUiState()
    data class Ready(
        val folders: List<CloudFolder>,
        val totalBytes: Long,
        val bookCount: Int,
    ) : CloudUiState()
    data class Error(val msg: String) : CloudUiState()
}

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow<CloudUiState>(CloudUiState.Loading)
    val state: StateFlow<CloudUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CloudUiState.Loading
            var attempt = 0
            val maxAttempts = 3
            while (attempt < maxAttempts) {
                attempt++
                try {
                    val allBooks = mutableListOf<com.booknext.app.data.remote.dto.BookDto>()
                    var page = 1
                    while (true) {
                        val resp = apiClient.api().listBooks(page = page, pageSize = 100)
                        allBooks.addAll(resp.books)
                        if (allBooks.size >= resp.total) break
                        page++
                    }

                    val entities = allBooks.map { dto ->
                        BookEntity(
                            bookId = dto.bookId,
                            title = dto.title,
                            author = dto.author,
                            format = dto.format,
                            fileSize = dto.size,
                            uploadTime = 0L,
                            hasCover = dto.hasCover,
                            status = dto.status,
                            category = dto.category,
                            pageCount = dto.pageCount,
                        )
                    }
                    bookDao.upsertAll(entities)

                    val grouped = entities
                        .filter { it.category != "__ocr__" }
                        .groupBy { it.category.ifEmpty { "__root__" } }

                    val folders = mutableListOf<CloudFolder>()

                    grouped["__root__"]?.let { books ->
                        folders.add(CloudFolder("__root__", "未分类书籍", books))
                    }

                    grouped.entries
                        .filter { it.key != "__root__" }
                        .sortedBy { it.key }
                        .forEach { (name, books) ->
                            folders.add(CloudFolder(name, name, books))
                        }

                    val totalBytes = entities.sumOf { it.fileSize }

                    _state.value = CloudUiState.Ready(
                        folders = folders,
                        totalBytes = totalBytes,
                        bookCount = entities.size,
                    )
                    return@launch
                } catch (e: Exception) {
                    if (attempt >= maxAttempts) {
                        _state.value = CloudUiState.Error("加载失败：${e.message}")
                    } else {
                        delay(1000L * attempt)
                    }
                }
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            try {
                apiClient.api().deleteBook(bookId)
                bookDao.deleteById(bookId)
                load()
            } catch (_: Exception) {
            }
        }
    }

    fun downloadBooks(context: Context, books: List<BookEntity>, baseUrl: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val localDir = File(context.filesDir, "local_books")
            localDir.mkdirs()
            val client = OkHttpClient()
            books.forEach { book ->
                try {
                    val url = "${baseUrl}/api/stream/${book.bookId}?k=$apiKey"
                    val ext = book.format.ifEmpty { "epub" }
                    val safeName = book.title.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                    val destFile = File(localDir, "$safeName.$ext")
                    if (destFile.exists()) return@forEach
                    val request = okhttp3.Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        response.close()
                        _state.value = CloudUiState.Error("下载失败：${book.title} — HTTP ${response.code}")
                        return@forEach
                    }
                    response.body?.byteStream()?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    bookDao.upsert(book.copy(
                        filePath = destFile.absolutePath,
                        isDownloaded = true,
                    ))
                } catch (_: Exception) {}
            }
        }
    }

    fun moveBook(bookId: String, folderName: String) {
        viewModelScope.launch {
            try {
                val body = folderName.toRequestBody("text/plain".toMediaType())
                apiClient.api().updateBook(id = bookId, category = body)
                bookDao.updateCategory(bookId, folderName)
            } catch (_: Exception) {}
        }
    }
}
