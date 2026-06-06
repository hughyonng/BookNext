package com.booknext.app.ui.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LocalBook(
    val path: String,
    val name: String,
    val format: String,
    val size: Long,
    val bookId: String = "",
    val isFavorite: Boolean = false,
)

@HiltViewModel
class LocalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
) : ViewModel() {

    private val localDir = File(context.filesDir, "local_books")
    private val _localBooks = MutableStateFlow<List<LocalBook>>(emptyList())
    val localBooks: StateFlow<List<LocalBook>> = _localBooks

    init {
        localDir.mkdirs()
        loadLocalBooks()
    }

    private fun loadLocalBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val books = localDir.listFiles()
                ?.filter { it.extension in listOf("epub", "pdf", "txt", "mobi", "azw3") }
                ?.map { file ->
                    val bookId = "local_${file.nameWithoutExtension}"
                    val entity = runCatching { bookDao.getById(bookId) }.getOrNull()
                    LocalBook(
                        path = file.absolutePath,
                        name = file.nameWithoutExtension,
                        format = file.extension,
                        size = file.length(),
                        bookId = bookId,
                        isFavorite = entity?.isFavorite ?: false,
                    )
                } ?: emptyList()
            _localBooks.value = books.sortedByDescending { File(it.path).lastModified() }
        }
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(idx)
            } ?: "imported_book"

            var destFile = File(localDir, name)
            var counter = 1
            while (destFile.exists()) {
                val base = name.substringBeforeLast('.')
                val ext = name.substringAfterLast('.')
                destFile = File(localDir, "${base}(${counter}).${ext}")
                counter++
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            val ext = destFile.extension.lowercase()
            val bookId = "local_${destFile.nameWithoutExtension}"
            bookDao.upsert(BookEntity(
                bookId = bookId,
                title = destFile.nameWithoutExtension,
                author = "本地文件",
                format = ext,
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                isDownloaded = true,
                lastReadTime = System.currentTimeMillis(),
            ))

            loadLocalBooks()
        }
    }

    fun deleteLocal(path: String) {
        val file = File(path)
        val bookId = "local_${file.nameWithoutExtension}"
        file.delete()
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.deleteById(bookId)
            loadLocalBooks()
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch {
            bookDao.toggleFavorite(bookId)
            loadLocalBooks()
        }
    }
}
