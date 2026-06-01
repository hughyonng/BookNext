package com.booknext.app.ui.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val apiClient: ApiClient,
    private val prefs: UserPreferences,
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

    // 文件夹列表（合并书籍分类 + DataStore 空文件夹）
    val folders: StateFlow<List<String>> = combine(
        bookDao.observeAll(),
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
        bookDao.observeAll(), _selectedFolder, _searchQuery
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
            _syncState.value = SyncState.Loading
            try {
                val resp = apiClient.api().listBooks(page = 1, pageSize = 100)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val entities = resp.books.map { dto ->
                    val uploadTs = try {
                        dateFormat.parse(dto.uploadTime.take(19))?.time ?: 0L
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
                entities.forEach { entity ->
                    val old = bookDao.getById(entity.bookId)
                    if (old != null) {
                        bookDao.upsert(entity.copy(
                            lastReadAt = old.lastReadAt,
                            progress = old.progress,
                            readingPercent = old.readingPercent,
                            lastReadTime = old.lastReadTime,
                            totalReadingSeconds = old.totalReadingSeconds,
                            isFinished = old.isFinished,
                            isFavorite = old.isFavorite,
                        ))
                    } else {
                        bookDao.upsert(entity)
                    }
                }
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
                    val body = okhttp3.RequestBody.create(
                        "text/plain".toMediaType(), ""
                    )
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
                val body = okhttp3.RequestBody.create(
                    "text/plain".toMediaType(), folderName
                )
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
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data object Success : SyncState()
    data class Error(val msg: String) : SyncState()
}