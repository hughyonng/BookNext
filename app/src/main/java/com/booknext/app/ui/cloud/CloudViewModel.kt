package com.booknext.app.ui.cloud

import android.content.Context
import android.net.Uri
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
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

enum class TransferType { UPLOAD, DOWNLOAD }
enum class TransferStatus { RUNNING, SUCCESS, ERROR }

data class TransferItem(
    val id: String,
    val fileName: String,
    val type: TransferType,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val status: TransferStatus = TransferStatus.RUNNING,
    val errorMessage: String? = null,
)

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow<CloudUiState>(CloudUiState.Loading)
    val state: StateFlow<CloudUiState> = _state

    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers

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

    fun uploadFile(context: Context, uri: Uri) {
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(idx)
        } ?: "未知文件"

        val transferId = UUID.randomUUID().toString()
        addTransfer(TransferItem(id = transferId, fileName = fileName, type = TransferType.UPLOAD))

        viewModelScope.launch(Dispatchers.IO) {
            val ext = fileName.substringAfterLast('.', "bin")
            val tmpFile = File(context.cacheDir, "${UUID.randomUUID()}.$ext")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmpFile.outputStream().use { output -> input.copyTo(output) }
                }
                val totalBytes = tmpFile.length()
                updateTransfer(transferId) { it.copy(totalBytes = totalBytes) }

                val filePart = MultipartBody.Part.createFormData(
                    "file", tmpFile.name,
                    tmpFile.asRequestBody("application/octet-stream".toMediaType()),
                )
                val title = fileName.substringBeforeLast('.').toRequestBody("text/plain".toMediaType())
                val author = "未知".toRequestBody("text/plain".toMediaType())
                val ocr = "false".toRequestBody("text/plain".toMediaType())

                apiClient.api().uploadBook(filePart, title, author, ocr)
                updateTransfer(transferId) { it.copy(status = TransferStatus.SUCCESS, transferredBytes = totalBytes) }
                delay(1500)
                load()
            } catch (e: Exception) {
                updateTransfer(transferId) { it.copy(status = TransferStatus.ERROR, errorMessage = e.message) }
            } finally {
                tmpFile.delete()
            }
        }
    }

    fun downloadBooks(context: Context, books: List<BookEntity>, baseUrl: String, apiKey: String) {
        val localDir = File(context.filesDir, "local_books")
        localDir.mkdirs()
        val client = OkHttpClient()

        books.forEach { book ->
            val ext = book.format.ifEmpty { "epub" }
            val safeName = book.title.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val fileName = "$safeName.$ext"
            val transferId = UUID.randomUUID().toString()
            addTransfer(TransferItem(id = transferId, fileName = fileName, type = TransferType.DOWNLOAD, totalBytes = book.fileSize))

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val url = "${baseUrl}/api/stream/${book.bookId}?k=$apiKey"
                    val destFile = File(localDir, fileName)
                    if (destFile.exists()) {
                        updateTransfer(transferId) { it.copy(status = TransferStatus.SUCCESS, transferredBytes = book.fileSize) }
                        return@launch
                    }
                    val request = okhttp3.Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        updateTransfer(transferId) { it.copy(status = TransferStatus.ERROR, errorMessage = "HTTP ${response.code}") }
                        response.close()
                        return@launch
                    }
                    val body = response.body ?: run {
                        updateTransfer(transferId) { it.copy(status = TransferStatus.ERROR, errorMessage = "空响应") }
                        return@launch
                    }
                    val total = body.contentLength().coerceAtLeast(1L)
                    var transferred = 0L
                    body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            val buf = ByteArray(8192)
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
                                transferred += read
                                val t = transferred
                                updateTransfer(transferId) { it.copy(transferredBytes = t) }
                            }
                        }
                    }
                    bookDao.upsert(book.copy(filePath = destFile.absolutePath, isDownloaded = true))
                    updateTransfer(transferId) { it.copy(status = TransferStatus.SUCCESS, transferredBytes = total) }
                } catch (e: Exception) {
                    updateTransfer(transferId) { it.copy(status = TransferStatus.ERROR, errorMessage = e.message) }
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
            } catch (_: Exception) {}
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

    fun clearCompletedTransfers() {
        _transfers.value = _transfers.value.filter { it.status == TransferStatus.RUNNING }
    }

    private fun addTransfer(item: TransferItem) {
        _transfers.value = listOf(item) + _transfers.value
    }

    private fun updateTransfer(id: String, block: (TransferItem) -> TransferItem) {
        _transfers.value = _transfers.value.map { if (it.id == id) block(it) else it }
    }
}
