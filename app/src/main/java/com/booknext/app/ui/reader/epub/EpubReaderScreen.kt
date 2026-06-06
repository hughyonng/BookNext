package com.booknext.app.ui.reader.epub

import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
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

private var epubNavigatorRef: EpubNavigatorFragment? = null
private var epubPublicationRef: org.readium.r2.shared.publication.Publication? = null
private var epubSearchIndex: List<Pair<Int, String>>? = null
private var epubPendingHighlight: String = ""

private fun buildEpubSpannable(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    if (query.isBlank()) return builder.toAnnotatedString()
    var start = 0
    val lower = text.lowercase()
    val qLower = query.lowercase()
    while (true) {
        val idx = lower.indexOf(qLower, start)
        if (idx < 0) break
        builder.addStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red), idx, idx + qLower.length)
        start = idx + qLower.length
    }
    return builder.toAnnotatedString()
}

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
    // 浮层按钮状态
    var showFloatingMenu by remember { mutableStateOf(false) }
    var floatingText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val epubCtx = androidx.compose.ui.platform.LocalContext.current
    // 搜索状态
    var epubSearchQuery by remember { mutableStateOf("") }
    var epubSearchMatches by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var showEpubSearchResults by remember { mutableStateOf(false) }
    var showEpubBackToResults by remember { mutableStateOf(false) }
    var epubHighlightQuery by remember { mutableStateOf("") }
    var epubHighlightChapter by remember { mutableIntStateOf(-1) }
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
    onSelectionChanged = { text ->
        floatingText = text
        showFloatingMenu = text.isNotEmpty()
    },
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
            onTocJump = { idx ->
                val nav = epubNavigatorRef
                val pub = epubPublicationRef
                if (nav != null && pub != null) {
                    scope.launch(Dispatchers.Main) {
                        val link = pub.readingOrder.getOrNull(idx) ?: return@launch
                        val hrefStr = link.href.toString()
                        val url = org.readium.r2.shared.util.Url(hrefStr) ?: return@launch
                        val locator = Locator(
                            href = url,
                            mediaType = link.mediaType ?: MediaType.EPUB,
                        )
                        nav.go(locator, animated = false)
                    }
                }
            },
            onAddBookmark = { onAddBookmark(initialPage) },
            onSearch = { query, _ ->
                if (query.isNotBlank()) {
                    epubSearchQuery = query
                    epubHighlightQuery = query
                    val index = epubSearchIndex
                    if (index != null) {
                        epubSearchMatches = index.mapNotNull { (chIdx, text) ->
                            if (text.contains(query, ignoreCase = true)) chIdx to text else null
                        }
                    } else {
                        epubSearchMatches = emptyList()
                    }
                    showEpubSearchResults = true
                    showEpubBackToResults = false
                }
            },
            onSearchRequest = {
                if (epubSearchMatches.isNotEmpty()) {
                    showEpubSearchResults = true
                }
            },
            onReplaceAll = { _, _ ->
                Toast.makeText(epubCtx, "EPUB暂不支持替换功能", Toast.LENGTH_SHORT).show()
            },
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
        // 搜索结果覆盖层
        if (showEpubSearchResults) {
            val srBg = if (darkMode) Color(0xFF1A1814) else Color(0xFFF9F7F4)
            val srFg = if (darkMode) Color(0xFFE0D8CC) else Color(0xFF1A1A1A)
            Surface(
                color = srBg,
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索: $epubSearchQuery", color = srFg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text("共 ${epubSearchMatches.size} 个结果", color = srFg.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                        TextButton(onClick = {
                            showEpubSearchResults = false
                            showEpubBackToResults = false
                            epubSearchQuery = ""
                            epubSearchMatches = emptyList()
                            epubPendingHighlight = ""
                        }) { Text("清除", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { showEpubSearchResults = false; showEpubBackToResults = true }) { Icon(Icons.Default.Close, "关闭", tint = srFg) }
                    }
                    HorizontalDivider(color = srFg.copy(alpha = 0.2f))
                    if (epubSearchMatches.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("正在索引中，请稍后重试…", color = srFg.copy(alpha = 0.6f))
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            items(epubSearchMatches.size) { idx ->
                                val (chIdx, fullText) = epubSearchMatches[idx]
                                // 找到匹配位置周围的一段文字
                                val lower = fullText.lowercase()
                                val ql = epubSearchQuery.lowercase()
                                val matchPos = lower.indexOf(ql)
                                val displayText = if (matchPos >= 0) {
                                    val start = (matchPos - 40).coerceAtLeast(0)
                                    val end = (matchPos + ql.length + 40).coerceAtMost(fullText.length)
                                    val prefix = if (start > 0) "…" else ""
                                    val suffix = if (end < fullText.length) "…" else ""
                                    prefix + fullText.substring(start, end).trim() + suffix
                                } else fullText.take(150)
                                TextButton(onClick = {
                                    scope.launch {
                                        showEpubSearchResults = false
                                        epubHighlightChapter = chIdx
                                        epubPendingHighlight = epubSearchQuery
                                        showEpubBackToResults = true
                                        val nav = epubNavigatorRef
                                        val pubRef = epubPublicationRef
                                        if (nav != null && pubRef != null) {
                                            val link = pubRef.readingOrder.getOrNull(chIdx) ?: return@launch
                                            val hrefStr = link.href.toString()
                                            val url = org.readium.r2.shared.util.Url(hrefStr) ?: return@launch
                                            nav.go(Locator(href = url, mediaType = link.mediaType ?: MediaType.EPUB), animated = false)
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text("第 ${chIdx + 1} 章", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(buildEpubSpannable(displayText, epubSearchQuery), style = MaterialTheme.typography.bodyMedium, color = srFg, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                }
                                if (idx < epubSearchMatches.size - 1) HorizontalDivider(color = srFg.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
        // 返回搜索结果 FAB
        if (showEpubBackToResults) {
            FloatingActionButton(
                onClick = { showEpubBackToResults = false; showEpubSearchResults = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Search, "返回搜索结果", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        // 浮动菜单（可拖拽）
        if (showFloatingMenu) {
            var offsetX by remember { mutableFloatStateOf(40f) }
            var offsetY by remember { mutableFloatStateOf(120f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            // 点击空白区域关闭菜单
                            showFloatingMenu = false
                        }
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                ) {
                    Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = {
                            showFloatingMenu = false
                            onTextLongPress(floatingText)
                            onAnnotationsClick()
                        }) { Text("高亮") }
                        TextButton(onClick = {
                            showFloatingMenu = false
                            onTextLongPress(floatingText)
                            onNoteClick()
                        }) { Text("笔记") }
                        TextButton(onClick = {
                            showFloatingMenu = false
                            onTextLongPress(floatingText)
                            onTranslateText()
                        }) { Text("翻译") }
                        TextButton(onClick = {
                            showFloatingMenu = false
                            onTextLongPress(floatingText)
                            onDictionaryLookup()
                        }) { Text("词典") }
                        TextButton(onClick = {
                            showFloatingMenu = false
                            onTtsRequest(floatingText)
                        }) { Text("朗读") }
                        TextButton(onClick = { showFloatingMenu = false }) { Text("✕") }
                    }
                }
            }
        }
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
    onSelectionChanged: (String) -> Unit = {},
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
        modifier = modifier.statusBarsPadding().navigationBarsPadding(),
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
                        if (x >= w * 0.25f && x <= w * 0.75f && dy < 8f && dt < 300L) {
                            onToggleUI()
                        }
                    }
                    return super.dispatchTouchEvent(event)
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
                    onSelectionChanged = onSelectionChanged,
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
    onSelectionChanged: (String) -> Unit = {},
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
    epubPublicationRef = publication

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
                val href = link.href.toString().split('#')[0] // 去掉锚点
                val idx = publication.readingOrder.indexOfFirst { it.href.toString().split('#')[0] == href }.coerceAtLeast(0)
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
                // 搜索高亮+滚动（每次章节切换后执行）
                val hq = epubPendingHighlight
                if (hq.isNotEmpty()) {
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                        // 延迟200ms等WebView渲染完成后执行
                        kotlinx.coroutines.delay(200)
                        fun findWebView(v: android.view.View): WebView? {
                            if (v is WebView) return v
                            if (v is android.view.ViewGroup) {
                                for (i in 0 until v.childCount) {
                                    findWebView(v.getChildAt(i))?.let { return it }
                                }
                            }
                            return null
                        }
                        val wv = findWebView(container) ?: return@launch
                        val q = hq.replace("'", "\\'")
                        wv.evaluateJavascript(
                            "(function(){var q='" + q + "';if(!q)return;var walk=document.createTreeWalker(document.body,4,null,false);var nodes=[];while(walk.nextNode()){var n=walk.currentNode;if(n.nodeValue.toLowerCase().indexOf(q.toLowerCase())>=0)nodes.push(n);}var firstMatch=null;for(var i=nodes.length-1;i>=0;i--){var n=nodes[i];var p=n.parentNode;var t=n.nodeValue;var idx=t.toLowerCase().indexOf(q.toLowerCase());if(idx<0)continue;var before=document.createTextNode(t.substring(0,idx));var mark=document.createElement('span');mark.style.background='#FFEB3B';mark.style.color='red';mark.textContent=t.substring(idx,idx+q.length);var after=document.createTextNode(t.substring(idx+q.length));p.insertBefore(after,n);p.insertBefore(mark,n);p.insertBefore(before,n);p.removeChild(n);firstMatch=mark;}if(firstMatch)firstMatch.scrollIntoView({behavior:'auto',block:'center'});})();",
                            null,
                        )
                    }
                }
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
        // 保存 navigator 引用用于目录跳转
        activity.supportFragmentManager.executePendingTransactions()
        epubNavigatorRef = activity.supportFragmentManager
            .findFragmentById(container.id) as? EpubNavigatorFragment
        // 后台构建搜索索引
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val index = mutableListOf<Pair<Int, String>>()
            for (chIdx in publication.readingOrder.indices) {
                val link = publication.readingOrder.getOrNull(chIdx) ?: continue
                try {
                    val resource = publication.get(link)
                    val bytes = resource?.read()?.getOrNull() ?: continue
                    val html = bytes.toString(Charsets.UTF_8)
                    val text = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                    if (text.length > 30) {
                        index.add(chIdx to text)
                    }
                } catch (_: Exception) {}
            }
            epubSearchIndex = index
            android.util.Log.d("BookNext", "Search index built: ${index.size} chapters")
        }
        // 找 WebView 注入文字选择检测
        container.postDelayed({
            fun findWebView(v: android.view.View): WebView? {
                if (v is WebView) return v
                if (v is android.view.ViewGroup) {
                    for (i in 0 until v.childCount) {
                        findWebView(v.getChildAt(i))?.let { return it }
                    }
                }
                return null
            }
            val wv = findWebView(container) ?: return@postDelayed
            android.util.Log.d("BookNext", "EPUB WebView found, class=${wv.javaClass.name}")
            // 轮询检测选择
            val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
            var lastSel = ""
            val pollRunnable = object : Runnable {
                override fun run() {
                    // 文字选择检测
                    wv.evaluateJavascript("(function(){return window.getSelection().toString();})()") { sel ->
                        val text = if (!sel.isNullOrEmpty() && sel != "\"\"") sel.removeSurrounding("\"") else ""
                        if (text != lastSel) {
                            lastSel = text
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                                onSelectionChanged(text)
                                if (text.isNotEmpty()) onTextLongPress(text)
                            }
                        }
                    }
                    pollHandler.postDelayed(this, 300)
                }
            }
            pollHandler.post(pollRunnable)
        }, 500)
    }
}
