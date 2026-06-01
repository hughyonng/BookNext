package com.booknext.app.ui.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
            } catch (e: Exception) {
                _state.value = CloudUiState.Error("加载失败：${e.message}")
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
}
