package com.booknext.app.ui.reader.epub

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.booknext.app.LocalActivity
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.ui.reader.ReaderToolbarOverlay
import com.booknext.app.ui.reader.ReaderToolbarState
import com.booknext.app.ui.reader.TocEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

private val MENU_HIGHLIGHT = Menu.FIRST + 1
private val MENU_NOTE = Menu.FIRST + 2
private val MENU_TRANSLATE = Menu.FIRST + 3
private val MENU_DICT = Menu.FIRST + 4
private val MENU_TTS = Menu.FIRST + 5

@Composable
fun EpubReaderScreen(
    file: File,
    title: String,
    initialPage: Int = 0,
    fontSize: Int = 17,
    darkMode: Boolean = false,
    onBack: () -> Unit = {},
    onProgressChange: (Int) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    onFontSizeChange: (Int) -> Unit = {},
    onOrientationChange: (String) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onToggleUI: () -> Unit = {},
    onTtsRequest: (String) -> Unit = {},
    isTtsPlaying: Boolean = false,
    stopTts: () -> Unit = {},
    onSaveSetting: (key: String, value: Any) -> Unit = { _, _ -> },
    onSetTranslateEngine: (String) -> Unit = {},
    onSetTranslateTargetLang: (String) -> Unit = {},
    onTranslateText: () -> Unit = {},
    onDictionaryLookup: () -> Unit = {},
    onAnnotationsClick: () -> Unit = {},
    onNoteClick: () -> Unit = {},
    onTextLongPress: (String) -> Unit = {},
    book: BookEntity? = null,
    sessions: List<ReadingSessionEntity> = emptyList(),
    coverUrl: String? = null,
    onToggleFavorite: () -> Unit = {},
    currentVisualOptions: com.booknext.app.ui.reader.options.VisualOptions = com.booknext.app.ui.reader.options.VisualOptions(),
    currentControlOptions: com.booknext.app.ui.reader.options.ControlOptions = com.booknext.app.ui.reader.options.ControlOptions(),
    currentOtherOptions: com.booknext.app.ui.reader.options.OtherOptions = com.booknext.app.ui.reader.options.OtherOptions(),
    onSaveVisualOptions: (com.booknext.app.ui.reader.options.VisualOptions) -> Unit = {},
    onSaveControlOptions: (com.booknext.app.ui.reader.options.ControlOptions) -> Unit = {},
    onSaveOtherOptions: (com.booknext.app.ui.reader.options.OtherOptions) -> Unit = {},
    bookmarks: List<Int> = emptyList(),
    onAddBookmark: (Int) -> Unit = {},
) {
    var uiVisible by remember { mutableStateOf(true) }
    var totalPages by remember { mutableIntStateOf(1) }
    var tocEntries by remember { mutableStateOf<List<TocEntry>>(emptyList()) }
    var epubTtsPlaying by remember { mutableStateOf(false) }
    var ttsChapterText by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        key(darkMode, fontSize) {
        EpubReaderWrapper(
            file = file,
            fontSize = fontSize,
            darkMode = darkMode,
            initialPage = initialPage,
            onProgressChange = onProgressChange,
            onToggleUI = { uiVisible = !uiVisible },
    onTocEntries = { tocEntries = it },
    onTotalPages = { totalPages = it },
    onChapterText = { ttsChapterText = it },
    lineSpacing = currentVisualOptions.lineSpacing,
    fontFamilyValue = currentVisualOptions.fontFamily,
    onTextLongPress = onTextLongPress,
    onTranslateText = onTranslateText,
    onDictionaryLookup = onDictionaryLookup,
    onAnnotationsClick = onAnnotationsClick,
    onNoteClick = onNoteClick,
    onTtsRequest = onTtsRequest,
    modifier = Modifier.fillMaxSize(),
        )
        }
        ReaderToolbarOverlay(
            state = ReaderToolbarState(
                title = title,
                currentPage = initialPage,
                totalPages = totalPages,
                darkMode = darkMode,
                fontSize = fontSize,
                tocEntries = tocEntries,
                bookmarks = bookmarks,
                isTtsPlaying = epubTtsPlaying,
            ),
            visible = uiVisible,
            onBack = onBack,
            onDarkModeChange = onDarkModeChange,
            onFontSizeChange = onFontSizeChange,
            onBrightnessChange = onBrightnessChange,
            onOrientationChange = onOrientationChange,
            onPageChange = onProgressChange,
            onTtsStart = {
                epubTtsPlaying = true
                android.util.Log.d("BookNext", "EPUB TTS start, text='${ttsChapterText.take(100)}' length=${ttsChapterText.length}")
                if (ttsChapterText.isNotBlank()) {
                    onTtsRequest(ttsChapterText)
                } else {
                    // fallback: extract text from current page
                    onTtsRequest("EPUB朗读测试：当前页面文本提取中，请翻页后重试。")
                    android.util.Log.d("BookNext", "EPUB TTS fallback - no text extracted")
                }
            },
            onTtsStop = { epubTtsPlaying = false; stopTts() },
            onTocJump = onProgressChange,
            onAddBookmark = { onAddBookmark(initialPage) },
            onSearch = { _, _ -> },
            book = book,
            sessions = sessions,
            coverUrl = coverUrl,
            onToggleFavorite = onToggleFavorite,
            currentVisualOptions = currentVisualOptions,
            currentControlOptions = currentControlOptions,
            currentOtherOptions = currentOtherOptions,
            onSaveVisualOptions = onSaveVisualOptions,
            onSaveControlOptions = onSaveControlOptions,
            onSaveOtherOptions = onSaveOtherOptions,
            onSaveSetting = onSaveSetting,
            onSetTranslateEngine = onSetTranslateEngine,
            onSetTranslateTargetLang = onSetTranslateTargetLang,
            onTranslateText = onTranslateText,
            onDictionaryLookup = onDictionaryLookup,
        )
    }
}

@Composable
fun EpubReaderWrapper(
    file: File,
    fontSize: Int,
    darkMode: Boolean,
    lineSpacing: Float = 1.8f,
    fontFamilyValue: String = "serif",
    initialPage: Int,
    onProgressChange: (Int) -> Unit,
    onToggleUI: () -> Unit = {},
    onTocEntries: (List<TocEntry>) -> Unit = {},
    onTotalPages: (Int) -> Unit = {},
    onChapterText: (String) -> Unit = {},
    onTextLongPress: (String) -> Unit = {},
    onTranslateText: () -> Unit = {},
    onDictionaryLookup: () -> Unit = {},
    onAnnotationsClick: () -> Unit = {},
    onNoteClick: () -> Unit = {},
    onTtsRequest: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    if (error != null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val container = object : FrameLayout(ctx) {
                var downY = 0f
                var downTime = 0L
                override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        downY = event.y
                        downTime = event.eventTime
                    }
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val w = width.toFloat()
                        val x = event.x
                        val dy = kotlin.math.abs(event.y - downY)
                        val dt = event.eventTime - downTime
                        // 长按（>300ms）表示选择文字，不触发 toggle
                        if (x >= w * 0.25f && x <= w * 0.75f && dy < 8f && dt < 300L) {
                            onToggleUI()
                        }
                    }
                    return super.dispatchTouchEvent(event)
                }
                override fun startActionModeForChild(child: android.view.View?, callback: ActionMode.Callback?): ActionMode? {
                    val fl = this
                    val wrapped = if (callback != null) object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                            val r = callback.onCreateActionMode(mode, menu)
                            // 添加自定义按钮
                            if (menu.findItem(MENU_HIGHLIGHT) == null) {
                                menu.add(Menu.NONE, MENU_HIGHLIGHT, 1, "高亮")
                                menu.add(Menu.NONE, MENU_NOTE, 2, "笔记")
                                menu.add(Menu.NONE, MENU_TRANSLATE, 3, "翻译")
                                menu.add(Menu.NONE, MENU_DICT, 4, "词典")
                                menu.add(Menu.NONE, MENU_TTS, 5, "朗读")
                            }
                            return r
                        }
                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                            return callback.onPrepareActionMode(mode, menu)
                        }
                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                            if (callback.onActionItemClicked(mode, item)) return true
                            // 获取 WebView 选中文本
                            fun findWebView(v: android.view.View): WebView? {
                                if (v is WebView) return v
                                if (v is android.view.ViewGroup) {
                                    for (i in 0 until v.childCount) {
                                        findWebView(v.getChildAt(i))?.let { return it }
                                    }
                                }
                                return null
                            }
                            val wv = findWebView(fl)
                            wv?.evaluateJavascript("(function(){return window.getSelection().toString();})()") { sel ->
                                val text = if (!sel.isNullOrEmpty() && sel != "\"\"") sel.removeSurrounding("\"") else ""
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                                    onTextLongPress(text)
                                    when (item.itemId) {
                                        MENU_HIGHLIGHT -> onAnnotationsClick()
                                        MENU_NOTE -> onNoteClick()
                                        MENU_TRANSLATE -> onTranslateText()
                                        MENU_DICT -> onDictionaryLookup()
                                        MENU_TTS -> { onTtsRequest(text); mode.finish() }
                                    }
                                }
                            }
                            return true
                        }
                        override fun onDestroyActionMode(mode: ActionMode) {
                            callback.onDestroyActionMode(mode)
                        }
                    } else null
                    return super.startActionModeForChild(child, wrapped)
                }
            }.apply { id = android.view.View.generateViewId() }

            scope.launch {
                try {
                    openEpubPublication(
                        activity = activity,
                        file = file,
                        container = container,
                        fontSize = fontSize,
                        darkMode = darkMode,
                        lineSpacing = lineSpacing,
                        fontFamilyValue = fontFamilyValue,
                        initialPage = initialPage,
                    onProgressChange = onProgressChange,
                    onTocEntries = onTocEntries,
                    onTotalPages = onTotalPages,
                    onChapterText = onChapterText,
                    onTextLongPress = onTextLongPress,
                    onTranslateText = onTranslateText,
                    onDictionaryLookup = onDictionaryLookup,
                    onAnnotationsClick = onAnnotationsClick,
                    onNoteClick = onNoteClick,
                    onTtsRequest = onTtsRequest,
                    onError = { msg -> error = msg },
                    )
                } catch (e: Exception) {
                    error = "加载失败：${e.message}"
                }
            }

            container
        }
    )
}

private suspend fun openEpubPublication(
    activity: FragmentActivity,
    file: File,
    container: FrameLayout,
    fontSize: Int,
    darkMode: Boolean,
    lineSpacing: Float,
    fontFamilyValue: String,
    initialPage: Int,
    onProgressChange: (Int) -> Unit,
    onTocEntries: (List<TocEntry>) -> Unit = {},
    onTotalPages: (Int) -> Unit = {},
    onChapterText: (String) -> Unit = {},
    onTextLongPress: (String) -> Unit = {},
    onTranslateText: () -> Unit = {},
    onDictionaryLookup: () -> Unit = {},
    onAnnotationsClick: () -> Unit = {},
    onNoteClick: () -> Unit = {},
    onTtsRequest: (String) -> Unit = {},
    onError: (String) -> Unit,
) {
    val httpClient = org.readium.r2.shared.util.http.DefaultHttpClient()
    val assetRetriever = org.readium.r2.shared.util.asset.AssetRetriever(
        contentResolver = activity.contentResolver,
        httpClient = httpClient,
    )
    val publicationOpener = org.readium.r2.streamer.PublicationOpener(
        publicationParser = org.readium.r2.streamer.parser.DefaultPublicationParser(
            context = activity,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        )
    )

    val publication = publicationOpener.open(
        asset = assetRetriever.retrieve(file, org.readium.r2.shared.util.format.FormatHints()).fold(
            onSuccess = { it },
            onFailure = { err ->
                onError("文件读取失败：${err.message}")
                return
            }
        ),
        allowUserInteraction = false,
    ).fold(
        onSuccess = { it },
        onFailure = { err ->
            onError("EPUB 解析失败：${err.message}")
            return
        }
    )

    val prefs = EpubPreferences(
        fontSize = (fontSize / 16.0).coerceAtLeast(0.5),
        lineHeight = if (lineSpacing != 1.8f) lineSpacing.toDouble() else null,
        fontFamily = when (fontFamilyValue) {
            "sans-serif" -> org.readium.r2.navigator.preferences.FontFamily.SANS_SERIF
            "monospace" -> org.readium.r2.navigator.preferences.FontFamily.MONOSPACE
            else -> org.readium.r2.navigator.preferences.FontFamily.SERIF
        },
        theme = if (darkMode) Theme.DARK else Theme.LIGHT,
        scroll = true,
        publisherStyles = false,
    )

    val locator: Locator? = publication.readingOrder
        .getOrNull(initialPage)
        ?.let { link ->
            val hrefString = link.href.toString()
            Locator(
                href = org.readium.r2.shared.util.Url(hrefString)!!,
                mediaType = link.mediaType ?: org.readium.r2.shared.util.mediatype.MediaType.EPUB,
            )
        }

    // Extract table of contents
    fun flattenToc(links: List<org.readium.r2.shared.publication.Link>): List<TocEntry> {
        val result = mutableListOf<TocEntry>()
        for (link in links) {
            val title = link.title?.toString()?.trim()?.takeIf { it.isNotBlank() }
            if (title != null) {
                val href = link.href
                val idx = publication.readingOrder.indexOfFirst { it.href == href }.coerceAtLeast(0)
                result.add(TocEntry(title = title, index = idx))
            }
            result.addAll(flattenToc(link.children))
        }
        return result
    }
    val tocList = flattenToc(publication.tableOfContents)
    withContext(Dispatchers.Main) { onTocEntries(tocList) }
    withContext(Dispatchers.Main) { onTotalPages(publication.readingOrder.size) }

    val navigatorFactory = EpubNavigatorFactory(publication = publication)
    val fragmentFactory = navigatorFactory.createFragmentFactory(
        initialLocator = locator,
        initialPreferences = prefs,
        listener = object : EpubNavigatorFragment.Listener {
            override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {}
        },
        paginationListener = object : EpubNavigatorFragment.PaginationListener {
            override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                val chapterIndex = publication.readingOrder.indexOfFirst {
                    it.href.toString() == locator.href.toString()
                }.coerceAtLeast(0)
                onProgressChange(chapterIndex)
                // Extract text for TTS
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val link = publication.readingOrder.getOrNull(chapterIndex) ?: run {
                        android.util.Log.w("BookNext", "TTS: no link at index $chapterIndex"); return@launch
                    }
                    val resource = publication.get(link)
                    android.util.Log.d("BookNext", "TTS: resource=$resource for link=${link.href}")
                    val bytes = resource?.read()?.getOrNull()
                    android.util.Log.d("BookNext", "TTS: bytes=${bytes?.size}")
                    if (bytes != null) {
                        val html = bytes.toString(Charsets.UTF_8)
                        val text = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                        android.util.Log.d("BookNext", "TTS: text length=${text.length}")
                        if (text.length > 100) {
                            val safe = text.take(3000)
                            withContext(Dispatchers.Main) { onChapterText(safe) }
                        }
                    }
                }
            }
            override fun onPageLoaded() {}
        },
    )

    withContext(Dispatchers.Main) {
        activity.supportFragmentManager.fragmentFactory = fragmentFactory
        activity.supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .replace(container.id, EpubNavigatorFragment::class.java, Bundle())
            .commitAllowingStateLoss()
    }
}
