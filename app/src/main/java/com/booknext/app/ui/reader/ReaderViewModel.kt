package com.booknext.app.ui.reader

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.AnnotationDao
import com.booknext.app.data.local.db.AnnotationEntity
import com.booknext.app.data.local.db.ReadingSessionDao
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import com.booknext.app.ui.reader.options.VisualOptions
import com.booknext.app.ui.reader.options.ControlOptions
import com.booknext.app.ui.reader.options.OtherOptions
import android.provider.Settings
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.util.Locale
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
    private val sessionDao: ReadingSessionDao,
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
    val screenOrientation: StateFlow<String> = prefs.screenOrientation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")
    val brightness: StateFlow<Float> = prefs.brightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1f)

    // ── 可视选项 StateFlow ─────────────────────────────
    val textColor: StateFlow<String> = prefs.textColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val bgColor: StateFlow<String> = prefs.bgColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val fontBold: StateFlow<Boolean> = prefs.fontBold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontItalic: StateFlow<Boolean> = prefs.fontItalic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontUnderline: StateFlow<Boolean> = prefs.fontUnderline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontShadow: StateFlow<Boolean> = prefs.fontShadow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val textJustify: StateFlow<Boolean> = prefs.textJustify
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val chineseLayout: StateFlow<Boolean> = prefs.chineseLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val verticalMode: StateFlow<Boolean> = prefs.verticalMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val simplifiedTrad: StateFlow<String> = prefs.simplifiedTrad
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")
    val paraSpacing: StateFlow<Float> = prefs.paraSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.7f)
    val charSpacing: StateFlow<Float> = prefs.charSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val fontScale: StateFlow<Float> = prefs.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val paddingH: StateFlow<Int> = prefs.paddingH
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)
    val paddingTop: StateFlow<Int> = prefs.paddingTop
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    val paddingBottom: StateFlow<Int> = prefs.paddingBottom
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    val pageAnimation: StateFlow<String> = prefs.pageAnimation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")
    val customFontPath: StateFlow<String> = prefs.customFontPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ── 控制选项 StateFlow ─────────────────────────────
    val tapLeftAction: StateFlow<String> = prefs.tapLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "prev_page")
    val tapRightAction: StateFlow<String> = prefs.tapRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "next_page")
    val tapCenterAction: StateFlow<String> = prefs.tapCenterAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "toggle_ui")
    val tapZone: StateFlow<String> = prefs.tapZone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "three_zone")
    val nineZoneConfig: StateFlow<String> = prefs.nineZoneConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val longPressAction: StateFlow<String> = prefs.longPressAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "select_text")
    val volumeUpAction: StateFlow<String> = prefs.volumeUpAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "prev_page")
    val volumeDownAction: StateFlow<String> = prefs.volumeDownAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "next_page")
    val swipeLeftAction: StateFlow<String> = prefs.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")
    val swipeRightAction: StateFlow<String> = prefs.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")
    val swipeUpAction: StateFlow<String> = prefs.swipeUpAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")
    val swipeDownAction: StateFlow<String> = prefs.swipeDownAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")

    // ── 其他选项 StateFlow ─────────────────────────────
    val keepScreenOn: StateFlow<Boolean> = prefs.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val edgeSwipeBrightness: StateFlow<Boolean> = prefs.edgeSwipeBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val edgeSwipeFontSize: StateFlow<Boolean> = prefs.edgeSwipeFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val smartIndent: StateFlow<Boolean> = prefs.smartIndent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val removeExtraBlank: StateFlow<Boolean> = prefs.removeExtraBlank
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val blueLight: StateFlow<Boolean> = prefs.blueLight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val blueLightAmount: StateFlow<Float> = prefs.blueLightAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.3f)
    val showStatusBar: StateFlow<Boolean> = prefs.showStatusBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showNavBar: StateFlow<Boolean> = prefs.showNavBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showProgressBar: StateFlow<Boolean> = prefs.showProgressBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val keepLastLine: StateFlow<Boolean> = prefs.keepLastLine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val readingReminder: StateFlow<Boolean> = prefs.readingReminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val readingReminderMins: StateFlow<Int> = prefs.readingReminderMins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)
    val ttsSplitMode: StateFlow<String> = prefs.ttsSplitMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sentence")
    val allowTiltFlip: StateFlow<Boolean> = prefs.allowTiltFlip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val allowPinchFont: StateFlow<Boolean> = prefs.allowPinchFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val allowSwipeFlip: StateFlow<Boolean> = prefs.allowSwipeFlip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val epubUseBookFont: StateFlow<Boolean> = prefs.epubUseBookFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val epubDisableCss: StateFlow<Boolean> = prefs.epubDisableCss
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val epubShowAnnotations: StateFlow<Boolean> = prefs.epubShowAnnotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val translateEngine: StateFlow<String> = prefs.translateEngine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "google")
    val translateTargetLang: StateFlow<String> = prefs.translateTargetLang
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "zh-CN")
    val nameReplacements: StateFlow<String> = prefs.nameReplacements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ── 合并选项 StateFlow ────────────────────────────
    val visualOptions: StateFlow<VisualOptions> = combine(
        listOf<Flow<*>>(
            fontSize, lineSpacing, fontFamily, customFontPath,
        )
    ) { arr ->
        VisualOptions(
            fontSize = arr[0] as Int,
            lineSpacing = arr[1] as Float,
            fontFamily = arr[2] as String,
            customFontPath = arr[3] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisualOptions())
    val controlOptions: StateFlow<ControlOptions> = combine(
        listOf<Flow<*>>(
            tapLeftAction, tapRightAction, tapCenterAction, longPressAction,
            volumeUpAction, volumeDownAction, tapZone, nineZoneConfig,
            swipeLeftAction, swipeRightAction, swipeUpAction, swipeDownAction,
        )
    ) { arr ->
        ControlOptions(
            tapLeftAction = arr[0] as String, tapRightAction = arr[1] as String,
            tapCenterAction = arr[2] as String, longPressAction = arr[3] as String,
            volumeUpAction = arr[4] as String, volumeDownAction = arr[5] as String,
            tapZone = arr[6] as String, nineZoneConfig = arr[7] as String,
            swipeLeftAction = arr[8] as String, swipeRightAction = arr[9] as String,
            swipeUpAction = arr[10] as String, swipeDownAction = arr[11] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ControlOptions())
    val otherOptions: StateFlow<OtherOptions> = combine(
        listOf<Flow<*>>(
            showStatusBar, showNavBar, showProgressBar, keepScreenOn, edgeSwipeBrightness, edgeSwipeFontSize,
            keepLastLine, smartIndent, removeExtraBlank, blueLight, blueLightAmount,
            readingReminder, readingReminderMins, ttsSplitMode, allowTiltFlip,
            allowPinchFont, allowSwipeFlip, epubUseBookFont, epubDisableCss,
            epubShowAnnotations,
        )
    ) { arr ->
        OtherOptions(
            showStatusBar = arr[0] as Boolean, showNavBar = arr[1] as Boolean, showProgressBar = arr[2] as Boolean, keepScreenOn = arr[3] as Boolean,
            edgeSwipeBrightness = arr[4] as Boolean, edgeSwipeFontSize = arr[5] as Boolean,
            keepLastLine = arr[6] as Boolean, smartIndent = arr[7] as Boolean,
            removeExtraBlank = arr[8] as Boolean, blueLight = arr[9] as Boolean,
            blueLightAmount = arr[10] as Float,
            readingReminder = arr[11] as Boolean, readingReminderMins = arr[12] as Int,
            ttsSplitMode = arr[13] as String, allowTiltFlip = arr[14] as Boolean,
            allowPinchFont = arr[15] as Boolean, allowSwipeFlip = arr[16] as Boolean,
            epubUseBookFont = arr[17] as Boolean, epubDisableCss = arr[18] as Boolean,
            epubShowAnnotations = arr[19] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OtherOptions())

    // ── TTS ─────────────────────────────────────────────────────
    private val _ttsPlaying = MutableStateFlow(false)
    val ttsPlaying: StateFlow<Boolean> = _ttsPlaying
    private val _ttsLoading = MutableStateFlow(false)
    val ttsLoading: StateFlow<Boolean> = _ttsLoading
    private var cloudPlayer: MediaPlayer? = null
    private var localTts: TextToSpeech? = null
    private var pendingTtsText: String? = null

    private val _bookmarks = MutableStateFlow<List<Int>>(emptyList())
    val bookmarks: StateFlow<List<Int>> = _bookmarks
    private val _annotations = MutableStateFlow<List<AnnotationEntity>>(emptyList())
    val annotations: StateFlow<List<AnnotationEntity>> = _annotations
    private val _sessions = MutableStateFlow<List<ReadingSessionEntity>>(emptyList())
    val sessions: StateFlow<List<ReadingSessionEntity>> = _sessions
    private var sessionStartMs: Long = 0L
    private var sessionStartProgress: Float = 0f
    private var sessionStartChars: Long = 0L
    private var annotationsJob: Job? = null
    private var sessionsJob: Job? = null

    val coverUrl: String?
        get() {
            val b = _book.value ?: return null
            return if (b.coverPath?.startsWith("http") == true) b.coverPath
            else null
        }

    fun loadSessions(bookId: String) {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch(Dispatchers.IO) {
            sessionDao.observeSessions(bookId).collect { _sessions.value = it }
        }
    }
    fun toggleFavorite() {
        val b = _book.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.toggleFavorite(b.bookId)
        }
    }
    fun loadAnnotations(bookId: String) {
        annotationsJob?.cancel()
        annotationsJob = viewModelScope.launch(Dispatchers.IO) {
            annotationDao.observeByBook(bookId).collect { _annotations.value = it }
        }
    }
    fun saveAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.upsert(annotation) }
    }
    fun deleteAnnotation(id: String) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.deleteById(id) }
    }

    private val _ttsCloudVoice = MutableStateFlow("zh-CN-XiaoxiaoNeural")
    val ttsCloudVoice: StateFlow<String> = _ttsCloudVoice
    private val _ttsCloudRate = MutableStateFlow("+0%")
    val ttsCloudRate: StateFlow<String> = _ttsCloudRate
    private val _ttsCloudPitch = MutableStateFlow("+0Hz")
    val ttsCloudPitch: StateFlow<String> = _ttsCloudPitch

    private val _useLocalTts = MutableStateFlow(false)
    val useLocalTts: StateFlow<Boolean> = _useLocalTts

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _ttsCloudVoice.value = prefs.ttsCloudVoice.first()
            _ttsCloudRate.value = prefs.ttsCloudRate.first()
            _ttsCloudPitch.value = prefs.ttsCloudPitch.first()
        }
    }

    fun setTtsCloudVoice(voice: String) {
        _ttsCloudVoice.value = voice
        viewModelScope.launch { prefs.saveTtsCloudVoice(voice) }
    }
    fun setTtsCloudRate(rate: String) {
        _ttsCloudRate.value = rate
        viewModelScope.launch { prefs.saveTtsCloudRate(rate) }
    }
    fun setTtsCloudPitch(pitch: String) {
        _ttsCloudPitch.value = pitch
        viewModelScope.launch { prefs.saveTtsCloudPitch(pitch) }
    }
    fun setUseLocalTts(v: Boolean) { _useLocalTts.value = v }
    fun openTtsSettings() {
        val tried = listOf(
            "com.android.settings.TTS_SETTINGS",
            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "android.settings.TTS_SETTINGS",
        )
        for (action in tried) {
            try {
                val i = android.content.Intent(action)
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return
            } catch (_: Exception) {}
        }
        try {
            val i = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (_: Exception) {}
    }

    fun startTts(text: String) {
        if (_useLocalTts.value) {
            startLocalTts(text)
        } else {
            startCloudTts(text)
        }
    }

    private fun startCloudTts(text: String) {
        _ttsPlaying.value = true
        _ttsLoading.value = true
        val safe = text.take(3800)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val voice = _ttsCloudVoice.value
                val rate = _ttsCloudRate.value
                val pitch = _ttsCloudPitch.value
                val req = com.booknext.app.data.remote.dto.TtsRequest(
                    text = safe, voice = voice, rate = rate, pitch = pitch
                )
                val body = apiClient.api().tts(req)
                val bytes = body.bytes()

                withContext(Dispatchers.Main) {
                    try {
                        cloudPlayer?.release()
                        val tempFile = java.io.File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                        tempFile.writeBytes(bytes)
                        cloudPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                android.media.AudioAttributes.Builder()
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(tempFile.absolutePath)
                            setOnPreparedListener {
                                _ttsLoading.value = false
                                start()
                            }
                            setOnCompletionListener { _ttsPlaying.value = false }
                            setOnErrorListener { _, what, extra ->
                                _ttsLoading.value = false
                                _ttsPlaying.value = false
                                android.util.Log.e("BookNext", "MediaPlayer error what=$what extra=$extra")
                                true
                            }
                            prepare()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BookNext", "MediaPlayer setup: ${e.message}", e)
                        _ttsLoading.value = false
                        _ttsPlaying.value = false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BookNext", "Cloud TTS error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _ttsLoading.value = false
                    _ttsPlaying.value = false
                }
            }
        }
    }

    fun startLocalTts(text: String) {
        _ttsPlaying.value = true
        localTts?.stop()
        localTts?.shutdown()
        localTts = null

        // 读取系统设置的默认TTS引擎包名
        var enginePkg: String? = null
        try {
            enginePkg = android.provider.Settings.Secure.getString(
                context.contentResolver, "tts_default_synth"
            )
            android.util.Log.d("BookNext", "TTS default engine from settings: $enginePkg")
        } catch (_: Exception) {}

        val safe = safeTtsText(text)
        pendingTtsText = safe
        localTts = TextToSpeech(context, { status ->
            android.util.Log.d("BookNext", "Local TTS init status=$status engine=$enginePkg")
            if (status != TextToSpeech.SUCCESS) {
                android.util.Log.e("BookNext", "Local TTS init failed status=$status")
                _ttsPlaying.value = false
                return@TextToSpeech
            }
            val pending = pendingTtsText ?: return@TextToSpeech
            pendingTtsText = null
            localTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { _ttsPlaying.value = false }
                override fun onError(utteranceId: String?) { _ttsPlaying.value = false }
            })
            try {
                val result = localTts?.speak(safe, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
                android.util.Log.d("BookNext", "TTS speak result=$result")
            } catch (e: Exception) {
                android.util.Log.e("BookNext", "TTS speak error: ${e.message}")
                _ttsPlaying.value = false
            }
        }, enginePkg?.ifEmpty { null })
    }

    fun stopTts() {
        cloudPlayer?.apply { if (isPlaying) stop(); release() }
        cloudPlayer = null
        localTts?.stop()
        pendingTtsText = null
        _ttsLoading.value = false
        _ttsPlaying.value = false
        // 清理TTS缓存文件
        try {
            context.cacheDir.listFiles { f -> f.name.startsWith("tts_") && f.name.endsWith(".mp3") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun safeTtsText(text: String): String {
        val maxLen = (TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3800))
            .coerceAtMost(text.length)
        return text.substring(0, maxLen)
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
            // 加载书签
            _bookmarks.value = prefs.observeBookmarks(bookId).first()

            // 本地文件直接读取
            if (entity.filePath != null && File(entity.filePath).exists()) {
                val localFile = File(entity.filePath)
                deliverFile(entity, localFile)
                return@launch
            }

            val cacheFile = File(context.filesDir, "books/${bookId}.${entity.format}")
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

    fun toggleBookmark(page: Int) {
        val bookId = _book.value?.bookId ?: return
        val current = _bookmarks.value
        viewModelScope.launch {
            if (page in current) {
                val updated = current - page
                _bookmarks.value = updated
                prefs.saveBookmarks(bookId, updated)
            } else {
                val updated = (current + page).distinct()
                _bookmarks.value = updated
                prefs.saveBookmarks(bookId, updated)
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
            bookDao.updateProgressNumeric(bookId, page.toString(), System.currentTimeMillis())
        }
    }

    fun setDarkMode(enabled: Boolean) {
        android.util.Log.d("BookNext", "setDarkMode=$enabled")
        viewModelScope.launch { prefs.saveDarkMode(enabled) }
    }
    fun setFontSize(size: Int) {
        android.util.Log.d("BookNext", "setFontSize=$size")
        viewModelScope.launch { prefs.saveFontSize(size) }
    }
    fun setScreenOrientation(orientation: String) {
        android.util.Log.d("BookNext", "setScreenOrientation=$orientation")
        viewModelScope.launch { prefs.saveScreenOrientation(orientation) }
    }
    fun setBrightness(value: Float) {
        android.util.Log.d("BookNext", "setBrightness=$value")
        viewModelScope.launch { prefs.saveBrightness(value) }
    }
    fun setBgColor(hex: String) {
        viewModelScope.launch { prefs.saveBgColor(hex) }
    }
    fun setNameReplacements(json: String) {
        viewModelScope.launch { prefs.saveNameReplacements(json) }
    }
    fun setTranslateEngine(engine: String) {
        viewModelScope.launch { prefs.saveTranslateEngine(engine) }
    }
    fun setTranslateTargetLang(lang: String) {
        viewModelScope.launch { prefs.saveTranslateTargetLang(lang) }
    }
    fun getPageTextForCopy(): String {
        return ""
    }
    fun saveVisualOptions(opt: com.booknext.app.ui.reader.options.VisualOptions) {
        viewModelScope.launch {
            prefs.saveVisualOptions(
                fontSize = opt.fontSize, lineSpacing = opt.lineSpacing,
                fontFamily = opt.fontFamily,
            )
            prefs.saveCustomFontPath(opt.customFontPath)
        }
    }
    fun saveControlOptions(opt: com.booknext.app.ui.reader.options.ControlOptions) {
        viewModelScope.launch {
            prefs.saveControlOptions(
                tapLeftAction = opt.tapLeftAction, tapRightAction = opt.tapRightAction,
                tapCenterAction = opt.tapCenterAction, longPressAction = opt.longPressAction,
                volumeUpAction = opt.volumeUpAction, volumeDownAction = opt.volumeDownAction,
                tapZone = opt.tapZone, nineZoneConfig = opt.nineZoneConfig,
                swipeLeftAction = opt.swipeLeftAction, swipeRightAction = opt.swipeRightAction,
                swipeUpAction = opt.swipeUpAction, swipeDownAction = opt.swipeDownAction,
            )
        }
    }
    fun saveOtherOptions(opt: com.booknext.app.ui.reader.options.OtherOptions) {
        viewModelScope.launch {
            prefs.saveOtherOptions(
                showStatusBar = opt.showStatusBar, showNavBar = opt.showNavBar, showProgressBar = opt.showProgressBar, keepScreenOn = opt.keepScreenOn,
                edgeSwipeBrightness = opt.edgeSwipeBrightness,
                edgeSwipeFontSize = opt.edgeSwipeFontSize,
                keepLastLine = opt.keepLastLine, smartIndent = opt.smartIndent,
                removeExtraBlank = opt.removeExtraBlank, blueLight = opt.blueLight,
                blueLightAmount = opt.blueLightAmount,
                readingReminder = opt.readingReminder,
                readingReminderMins = opt.readingReminderMins,
                ttsSplitMode = opt.ttsSplitMode, allowTiltFlip = opt.allowTiltFlip,
                allowPinchFont = opt.allowPinchFont, allowSwipeFlip = opt.allowSwipeFlip,
                epubUseBookFont = opt.epubUseBookFont, epubDisableCss = opt.epubDisableCss,
                epubShowAnnotations = opt.epubShowAnnotations,
            )
        }
    }
    fun clearError() { _state.value = ReaderState.Idle }

    private suspend fun deliverFile(entity: BookEntity, file: File) {
        sessionStartMs = System.currentTimeMillis()
        sessionStartProgress = entity.readingPercent
        sessionStartChars = estimateCharsRead(entity, file)
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateLastReadAt(entity.bookId, System.currentTimeMillis())
        }
        if (entity.format in listOf("mobi", "azw3")) {
            val bookId = entity.bookId
            val convertedFile = File(context.filesDir, "books/${bookId}_converted.epub")
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
            if (entity.format in listOf("doc", "docx")) {
                val extracted = extractWordToTxt(file, context, entity.bookId)
                _state.value = ReaderState.Ready(extracted ?: file, "txt")
            } else {
                _state.value = ReaderState.Ready(file, entity.format)
            }
        }
    }

    private fun extractWordToTxt(file: File, context: Context, bookId: String): File? {
        return try {
            val outFile = File(context.filesDir, "books/${bookId}_extracted.txt")
            if (outFile.exists() && outFile.length() > 0) return outFile
            val text = XWPFDocument(file.inputStream()).use { doc ->
                doc.paragraphs.joinToString("\n") { it.text }
            }
            outFile.writeText(text)
            outFile
        } catch (e: Exception) { null }
    }

    private fun estimateTotalChars(book: BookEntity): Long {
        return when (book.format) {
            "txt" -> {
                val f = File(context.filesDir, "books/${book.bookId}.txt")
                if (f.exists()) f.length() / 2 else 100_000L
            }
            "epub" -> 100_000L
            "pdf" -> (book.pageCount ?: 200) * 500L
            else -> 50_000L
        }
    }

    private fun estimateCharsRead(entity: BookEntity, file: File): Long {
        return (entity.readingPercent * estimateTotalChars(entity)).toLong()
    }

    override fun onCleared() {
        localTts?.shutdown()
        localTts = null
        cloudPlayer?.release()
        cloudPlayer = null
        super.onCleared()
        val bookId = _book.value?.bookId ?: return
        val elapsed = (System.currentTimeMillis() - sessionStartMs) / 1000
        val endProgress = _book.value?.readingPercent ?: 0f
        val bookSnapshot = _book.value ?: return

        if (elapsed > 5) {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                bookDao.addReadingTime(bookId, elapsed)
                val charsRead = ((endProgress - sessionStartProgress) * estimateTotalChars(bookSnapshot)).toLong()
                    .coerceAtLeast(0L)
                val wpm = if (elapsed > 0) ((charsRead / elapsed.toFloat()) * 60).toInt() else 0
                val session = ReadingSessionEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = bookId,
                    startTime = sessionStartMs,
                    durationSeconds = elapsed,
                    progressPercent = endProgress,
                    wordsPerMinute = wpm,
                    charsRead = charsRead,
                )
                sessionDao.insert(session)
            }
        }
    }
}