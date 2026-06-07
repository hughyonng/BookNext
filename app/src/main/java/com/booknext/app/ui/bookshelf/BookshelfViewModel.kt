package com.booknext.app.ui.bookshelf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.ApiClient
import com.booknext.app.data.remote.MetadataService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val apiClient: ApiClient,
    private val prefs: UserPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val books: StateFlow<List<BookEntity>> = bookDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    // 文件夹筛选
    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder

    // 搜索
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 元数据补全
    private val _metadataState = MutableStateFlow<MetadataState>(MetadataState.Idle)
    val metadataState: StateFlow<MetadataState> = _metadataState
    private val metadataService = MetadataService()

    // 文件夹列表（合并书籍分类 + DataStore 空文件夹）
    val folders: StateFlow<List<String>> = combine(
        books,
        prefs.emptyFolders,
    ) { books, emptyFolders ->
        val fromBooks = books
            .mapNotNull { it.category.ifEmpty { null } }
            .filter { it != "__ocr__" }
            .toSet()
        (fromBooks + emptyFolders).sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 筛选 + 搜索后的书单
    val filteredBooks: StateFlow<List<BookEntity>> = combine(
        books, _selectedFolder, _searchQuery
    ) { books, folder, query ->
        books.filter { book ->
            val matchFolder = folder == null || book.category == folder
            val matchQuery = query.isEmpty() ||
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true)
            matchFolder && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 最近阅读
    val recentBooks: StateFlow<List<BookEntity>> = bookDao.observeRecentlyRead()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncBooks()
    }

    fun syncBooks() {
        viewModelScope.launch {
            val url = prefs.serverUrl.first()
            if (url.isEmpty()) return@launch
            _syncState.value = SyncState.Loading
            try {
                var page = 1
                val allRemoteBooks = mutableListOf<com.booknext.app.data.remote.dto.BookDto>()
                while (true) {
                    val resp = apiClient.api().listBooks(page = page, pageSize = 100)
                    allRemoteBooks.addAll(resp.books)
                    if (resp.books.size < 100) break
                    page++
                }
                val fmtT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val fmtS = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val entities = allRemoteBooks.map { dto ->
                    val uploadTs = try {
                        val raw = dto.uploadTime.take(19)
                        val t = fmtT.parse(raw)?.time
                        if (t != null) t else fmtS.parse(raw.replace("T", " "))?.time ?: 0L
                    } catch (_: Exception) { 0L }
                    BookEntity(
                        bookId = dto.bookId,
                        title = dto.title,
                        author = dto.author,
                        format = dto.format,
                        fileSize = dto.size,
                        uploadTime = uploadTs,
                        category = dto.category,
                        hasCover = dto.hasCover,
                        status = dto.status,
                        pageCount = dto.pageCount,
                    )
                }
                val localMap = bookDao.observeAll().first().associateBy { it.bookId }
                entities.forEach { entity ->
                    val old = localMap[entity.bookId]
                    if (old != null) {
                        bookDao.upsert(entity.copy(
                            lastReadAt = old.lastReadAt,
                            progress = old.progress,
                            readingPercent = old.readingPercent,
                            lastReadTime = old.lastReadTime,
                            totalReadingSeconds = old.totalReadingSeconds,
                            isFinished = old.isFinished,
                            isFavorite = old.isFavorite,
                            filePath = old.filePath,
                            isDownloaded = old.isDownloaded,
                            pendingSync = old.pendingSync,
                            readingSessionStart = old.readingSessionStart,
                            lastSyncTime = old.lastSyncTime,
                            category = old.category,
                        ))
                    } else {
                        bookDao.upsert(entity)
                    }
                }
                val remoteIds = allRemoteBooks.map { it.bookId }.toSet()
                val localAll = bookDao.observeAll().first()
                localAll.filter { it.bookId !in remoteIds && !it.bookId.startsWith("local_") }
                    .forEach { bookDao.deleteById(it.bookId) }
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "同步失败")
            }
        }
    }

    fun selectFolder(folder: String?) { _selectedFolder.value = folder }
    fun onSearch(query: String) { _searchQuery.value = query }

    fun createFolder(name: String) {
        viewModelScope.launch { prefs.addEmptyFolder(name) }
    }

    fun deleteFolder(name: String) {
        viewModelScope.launch {
            val booksInFolder = bookDao.getByCategory(name)
            booksInFolder.forEach { book ->
                bookDao.updateCategory(book.bookId, "")
                try {
                    val body = "".toRequestBody("text/plain".toMediaType())
                    apiClient.api().updateBook(id = book.bookId, category = body)
                } catch (_: Exception) {}
            }
            prefs.removeEmptyFolder(name)
        }
    }

    fun addBookToFolder(bookId: String, folderName: String) {
        viewModelScope.launch {
            bookDao.updateCategory(bookId, folderName)
            try {
                val body = folderName.toRequestBody("text/plain".toMediaType())
                apiClient.api().updateBook(id = bookId, category = body)
            } catch (_: Exception) {}
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            try {
                apiClient.api().deleteBook(bookId)
            } catch (_: Exception) {}
            bookDao.deleteById(bookId)
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch { bookDao.toggleFavorite(bookId) }
    }

    fun autoFillMetadata(apiKey: String) {
        if (apiKey.isBlank()) return
        _metadataState.value = MetadataState.Running(0, 0)
        viewModelScope.launch(Dispatchers.IO) {
            prefs.saveGoogleBooksApiKey(apiKey)
            val allBooks = bookDao.observeAll().first()
            val needsFill = allBooks.filter {
                it.author.isNullOrEmpty() || it.author == "未知"
            }
            val total = needsFill.size
            if (total == 0) {
                _metadataState.value = MetadataState.Idle
                return@launch
            }
            var updated = 0
            for ((i, book) in needsFill.withIndex()) {
                _metadataState.value = MetadataState.Running(updated, total)
                try {
                    val meta = metadataService.lookup(book.title, apiKey)
                    if (meta != null) {
                        val author = if (meta.authors.isNotEmpty()) meta.authors.joinToString("、") else book.author
                        var coverPath = book.coverPath
                        if (meta.coverUrl != null) {
                            try {
                                val bytes = metadataService.downloadCover(meta.coverUrl)
                                if (bytes != null) {
                                    val coverFile = File(context.filesDir, "covers/${book.bookId}.jpg")
                                    coverFile.parentFile?.mkdirs()
                                    coverFile.writeBytes(bytes)
                                    coverPath = coverFile.absolutePath
                                }
                            } catch (_: Exception) {}
                        }
                        bookDao.upsert(book.copy(author = author, coverPath = coverPath ?: book.coverPath))
                        updated++
                    }
                } catch (_: Exception) {}
                if (i < total - 1) kotlinx.coroutines.delay(250)
            }
            _metadataState.value = MetadataState.Done(updated)
        }
    }

    fun resetMetadataState() { _metadataState.value = MetadataState.Idle }

    fun saveCoverFromUri(bookId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val coverFile = File(context.filesDir, "covers/$bookId.jpg")
            coverFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                coverFile.outputStream().use { output -> input.copyTo(output) }
            }
            bookDao.updateCoverPath(bookId, coverFile.absolutePath)
        }
    }

    fun extractCoverFromFile(bookId: String, filePath: String, format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = when (format.lowercase()) {
                "epub" -> extractEpubCover(filePath)
                "pdf" -> extractPdfCover(filePath)
                else -> null
            }
            if (bytes != null) {
                val coverFile = File(context.filesDir, "covers/$bookId.jpg")
                coverFile.parentFile?.mkdirs()
                coverFile.writeBytes(bytes)
                bookDao.updateCoverPath(bookId, coverFile.absolutePath)
            }
        }
    }

    private fun extractEpubCover(epubPath: String): ByteArray? {
        return try {
            val zip = ZipFile(epubPath)
            val coverNames = listOf(
                "cover.jpg", "cover.png", "cover.jpeg",
                "OEBPS/cover.jpg", "OEBPS/cover.png",
                "OEBPS/images/cover.jpg", "OEBPS/Images/cover.jpg",
                "images/cover.jpg", "Images/cover.jpg",
                "META-INF/cover.jpg",
            )
            for (name in coverNames) {
                val entry = zip.getEntry(name) ?: continue
                return zip.getInputStream(entry).readBytes()
            }
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (!e.isDirectory && e.name.contains("cover", ignoreCase = true)
                    && (e.name.endsWith(".jpg") || e.name.endsWith(".jpeg") || e.name.endsWith(".png"))) {
                    return zip.getInputStream(e).readBytes()
                }
            }
            zip.close()
            null
        } catch (_: Exception) { null }
    }

    private fun extractPdfCover(pdfPath: String): ByteArray? {
        return try {
            val file = File(pdfPath)
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount < 1) { renderer.close(); pfd.close(); return null }
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        } catch (_: Exception) { null }
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data object Success : SyncState()
    data class Error(val msg: String) : SyncState()
}

sealed class MetadataState {
    data object Idle : MetadataState()
    data class Running(val current: Int, val total: Int) : MetadataState()
    data class Done(val updated: Int) : MetadataState()
}