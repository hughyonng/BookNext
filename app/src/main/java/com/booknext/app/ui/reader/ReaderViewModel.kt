package com.booknext.app.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.AnnotationDao
import com.booknext.app.data.local.db.AnnotationEntity
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import javax.inject.Inject

sealed class ReaderState {
    data object Idle : ReaderState()
    data object Downloading : ReaderState()
    data class Ready(val file: File, val format: String) : ReaderState()
    data class Error(val msg: String) : ReaderState()
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val bookDao: BookDao,
    private val annotationDao: AnnotationDao,
    private val prefs: UserPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress

    val darkMode: StateFlow<Boolean> = prefs.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontSize: StateFlow<Int> = prefs.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 17)
    val fontFamily: StateFlow<String> = prefs.fontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "serif")
    val lineSpacing: StateFlow<Float> = prefs.lineSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.8f)

    private val _ttsPlaying = MutableStateFlow(false)
    val ttsPlaying: StateFlow<Boolean> = _ttsPlaying
    private var _ttsJob: Job? = null

    private val _annotations = MutableStateFlow<List<AnnotationEntity>>(emptyList())
    val annotations: StateFlow<List<AnnotationEntity>> = _annotations

    private var sessionStartMs: Long = 0L

    fun loadAnnotations(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            annotationDao.observeByBook(bookId).collect { _annotations.value = it }
        }
    }

    fun saveAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.upsert(annotation) }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.deleteById(id) }
    }

    fun loadBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = ReaderState.Downloading
            val entity = bookDao.getById(bookId)
            if (entity == null) {
                _state.value = ReaderState.Error("书籍信息不存在")
                return@launch
            }
            _book.value = entity
            _progress.value = entity.progress

            val cacheFile = File(context.cacheDir, "books/${bookId}.${entity.format}")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                deliverFile(entity, cacheFile)
                return@launch
            }

            try {
                val body: ResponseBody = apiClient.api().streamBook(bookId)
                cacheFile.parentFile?.mkdirs()
                cacheFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                deliverFile(entity, cacheFile)
            } catch (e: Exception) {
                _state.value = ReaderState.Error("下载失败：${e.message}")
            }
        }
    }

    fun saveProgress(locatorJson: String) {
        val bookId = _book.value?.bookId ?: return
        _progress.value = locatorJson
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateProgress(bookId, locatorJson, 0f, System.currentTimeMillis())
        }
    }

    fun savePageProgress(page: Int) {
        val bookId = _book.value?.bookId ?: return
        _progress.value = page.toString()
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateProgressNumeric(bookId, page.toString())
        }
    }

    fun startTts(text: String) {
        _ttsJob?.cancel()
        _ttsJob = viewModelScope.launch(Dispatchers.IO) {
            _ttsPlaying.value = true
            try {
                val req = com.booknext.app.data.remote.dto.TtsRequest(text = text)
                val body = apiClient.api().tts(req)
                val player = android.media.MediaPlayer()
                val tmpFile = File(context.cacheDir, "tts_tmp.mp3")
                tmpFile.outputStream().use { body.byteStream().copyTo(it) }
                player.setDataSource(tmpFile.absolutePath)
                player.prepare()
                player.start()
                player.setOnCompletionListener { _ttsPlaying.value = false; it.release() }
            } catch (e: Exception) { _ttsPlaying.value = false }
        }
    }

    fun stopTts() { _ttsJob?.cancel(); _ttsPlaying.value = false }
    fun clearError() { _state.value = ReaderState.Idle }

    private suspend fun deliverFile(entity: BookEntity, file: File) {
        sessionStartMs = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateLastReadAt(entity.bookId, System.currentTimeMillis())
        }
        if (entity.format in listOf("mobi", "azw3")) {
            val bookId = entity.bookId
            val convertedFile = File(context.cacheDir, "books/${bookId}_converted.epub")
            if (convertedFile.exists() && convertedFile.length() > 0) {
                _state.value = ReaderState.Ready(convertedFile, "epub")
                return
            }
            try {
                val body = apiClient.api().convertBook(bookId)
                convertedFile.parentFile?.mkdirs()
                convertedFile.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
                _state.value = ReaderState.Ready(convertedFile, "epub")
            } catch (e: Exception) {
                _state.value = ReaderState.Error("MOBI 转换失败：${e.message}")
            }
        } else {
            _state.value = ReaderState.Ready(file, entity.format)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val bookId = _book.value?.bookId ?: return
        val elapsed = (System.currentTimeMillis() - sessionStartMs) / 1000
        if (elapsed > 5) {
            viewModelScope.launch(Dispatchers.IO) {
                bookDao.addReadingTime(bookId, elapsed)
            }
        }
    }
}