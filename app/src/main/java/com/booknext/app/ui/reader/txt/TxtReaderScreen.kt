package com.booknext.app.ui.reader.txt

import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.booknext.app.ui.reader.ReaderToolbarOverlay
import com.booknext.app.ui.reader.ReaderToolbarState
import com.booknext.app.ui.reader.TocEntry
import com.booknext.app.ui.reader.TranslationDialog
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.AnnotationEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.util.EncodingDetector
import org.json.JSONArray
import java.io.File

private data class NameReplaceRule(val from: String, val to: String)

private fun parseNameReplacements(json: String): List<NameReplaceRule> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            NameReplaceRule(obj.getString("from"), obj.getString("to"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun applyNameReplacements(text: String, rules: List<NameReplaceRule>): String {
    var result = text
    for (rule in rules) {
        result = result.replace(rule.from, rule.to)
    }
    return result
}

private val MENU_HIGHLIGHT = Menu.FIRST + 1
private val MENU_NOTE = Menu.FIRST + 2
private val MENU_TTS = Menu.FIRST + 3
private val MENU_TRANSLATE = Menu.FIRST + 4
private val MENU_DICT = Menu.FIRST + 5

@Composable
fun TxtReaderScreen(
    file: File,
    title: String,
    initialPage: Int,
    fontSize: Int = 17,
    darkMode: Boolean = false,
    onBack: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onTtsRequest: (String) -> Unit = {},
    isTtsPlaying: Boolean = false,
    onTtsStop: () -> Unit = {},
    onAnnotationsClick: () -> Unit = {},
    onNoteClick: () -> Unit = {},
    onTextLongPress: (String, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onDarkModeChange: (Boolean) -> Unit = {},
    onFontSizeChange: (Int) -> Unit = {},
    book: BookEntity? = null,
    sessions: List<ReadingSessionEntity> = emptyList(),
    annotations: List<AnnotationEntity> = emptyList(),
    coverUrl: String? = null,
    onToggleFavorite: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    readerBgColor: String = "",
    showStatusBar: Boolean = false,
    showNavBar: Boolean = false,
    showProgressBar: Boolean = false,
    onSetReaderBgColor: (String) -> Unit = {},
    onPageTextCopy: () -> Unit = {},
    onBookmarkManage: () -> Unit = {},
    currentVisualOptions: com.booknext.app.ui.reader.options.VisualOptions = com.booknext.app.ui.reader.options.VisualOptions(),
    currentControlOptions: com.booknext.app.ui.reader.options.ControlOptions = com.booknext.app.ui.reader.options.ControlOptions(),
    currentOtherOptions: com.booknext.app.ui.reader.options.OtherOptions = com.booknext.app.ui.reader.options.OtherOptions(),
    onSaveVisualOptions: (com.booknext.app.ui.reader.options.VisualOptions) -> Unit = {},
    onSaveControlOptions: (com.booknext.app.ui.reader.options.ControlOptions) -> Unit = {},
    onSaveOtherOptions: (com.booknext.app.ui.reader.options.OtherOptions) -> Unit = {},
    onSaveSetting: (key: String, value: Any) -> Unit = { _, _ -> },
    onSetTranslateEngine: (String) -> Unit = {},
    onSetTranslateTargetLang: (String) -> Unit = {},
    onTranslateText: () -> Unit = {},
    onDictionaryLookup: () -> Unit = {},
    nameReplacements: String = "",
    onNameReplaceChange: (String) -> Unit = {},
    smartIndent: Boolean = true,
    removeExtraBlank: Boolean = false,
    fontFamily: String = "serif",
    lineSpacing: Float = 1.8f,
    bookmarks: List<Int> = emptyList(),
    onAddBookmark: (Int) -> Unit = {},
    onOrientationChange: (String) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
) {
    val rawLines by produceState<List<String>>(emptyList(), file) {
        value = withContext(Dispatchers.IO) { EncodingDetector.readLines(file) }
    }
    val nameRules = remember(nameReplacements) { parseNameReplacements(nameReplacements) }

    val lines = remember(rawLines, nameRules, smartIndent, removeExtraBlank) {
        rawLines.map { line ->
            var text = line
            if (nameRules.isNotEmpty()) text = applyNameReplacements(text, nameRules)
            if (smartIndent && text.isNotEmpty() && !text.startsWith(" ") && !text.startsWith("\t")) {
                text = "    $text"
            }
            text
        }.let { list ->
            if (removeExtraBlank) {
                list.filterIndexed { index, s ->
                    if (s.isBlank()) {
                        index == 0 || index == list.size - 1 || list.getOrNull(index - 1)?.isNotBlank() == true
                    } else true
                }
            } else list
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val bgColor = if (readerBgColor.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(readerBgColor)) } catch (_: Exception) { if (darkMode) Color(0xFF1A1814) else Color(0xFFF9F7F4) }
    } else if (darkMode) Color(0xFF1A1814) else Color(0xFFF9F7F4)
    var uiVisible by remember { mutableStateOf(true) }
    val tocEntries = remember(lines) {
        lines.mapIndexedNotNull { idx, line ->
            val trimmed = line.trim()
            val isChapter = trimmed.length in 2..40 && (
                trimmed.startsWith("第") ||
                trimmed.startsWith("Chapter") ||
                trimmed.startsWith("CHAPTER") ||
                trimmed.startsWith("chapter") ||
                trimmed.matches(Regex("^\\d+[.、．].*")) ||
                trimmed.matches(Regex("^[一二三四五六七八九十百]+[、.．章节].*"))
            )
            if (isChapter) TocEntry(title = trimmed, index = idx) else null
        }
    }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    var currentFontSize by remember { mutableIntStateOf(fontSize) }
    LaunchedEffect(fontSize) { if (fontSize != currentFontSize) currentFontSize = fontSize }

    var displayLines by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(lines) { displayLines = lines }

    var showTranslation by remember { mutableStateOf(false) }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var translateError by remember { mutableStateOf<String?>(null) }

    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var showBackToResults by remember { mutableStateOf(false) }
    // 当前页高亮标记（搜索结果跳转后使用）
    var highlightLine by remember { mutableIntStateOf(-1) }
    var highlightQuery by remember { mutableStateOf("") }

    fun pageSize(): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        return if (visible.isEmpty()) 16 else visible.size
    }
    suspend fun prevPage() { val ps = pageSize(); listState.animateScrollToItem((listState.firstVisibleItemIndex - ps).coerceAtLeast(0)) }
    suspend fun nextPage() { val ps = pageSize(); listState.animateScrollToItem((listState.firstVisibleItemIndex + ps).coerceAtMost(lines.size - 1)) }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onProgressChange(listState.firstVisibleItemIndex)
    }

    val textColor = remember(readerBgColor, darkMode) {
        if (readerBgColor.isNotBlank()) {
            try {
                val c = android.graphics.Color.parseColor(readerBgColor)
                val bri = (0.299 * android.graphics.Color.red(c) + 0.587 * android.graphics.Color.green(c) + 0.114 * android.graphics.Color.blue(c)) / 255.0
                if (bri < 0.5) android.graphics.Color.parseColor("#E0D8CC") else android.graphics.Color.parseColor("#1A1A1A")
            } catch (_: Exception) {
                if (darkMode) android.graphics.Color.parseColor("#E0D8CC") else android.graphics.Color.parseColor("#1A1A1A")
            }
        } else if (darkMode) android.graphics.Color.parseColor("#E0D8CC") else android.graphics.Color.parseColor("#1A1A1A")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = bgColor,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 80.dp),
            ) {
                items(displayLines.size) { i ->
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                setTextIsSelectable(true)
                                highlightColor = android.graphics.Color.parseColor("#4066BBFF")
                                setOnTouchListener { v, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_UP -> {
                                            val dt = event.eventTime - event.downTime
                                            if (dt > 0L && dt < 300L) {
                                                scope.launch {
                                                    val w = v.width.toFloat()
                                                    val x = event.x
                                                    when {
                                                        x < w * 0.25f -> prevPage()
                                                        x > w * 0.75f -> nextPage()
                                                        else -> uiVisible = !uiVisible
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    false
                                }
                                customSelectionActionModeCallback = object : ActionMode.Callback {
                                    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                                    var selStart = 0
                                    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                                    var selEnd = 0
                                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true
                                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                                        if (menu.findItem(MENU_HIGHLIGHT) == null) {
                                            menu.add(Menu.NONE, MENU_HIGHLIGHT, 1, "高亮")
                                            menu.add(Menu.NONE, MENU_NOTE, 2, "笔记")
                                            menu.add(Menu.NONE, MENU_TRANSLATE, 3, "翻译")
                                            menu.add(Menu.NONE, MENU_DICT, 4, "词典")
                                            menu.add(Menu.NONE, MENU_TTS, 5, "朗读")
                                        }
                                        selStart = selectionStart.coerceAtLeast(0)
                                        selEnd = selectionEnd.coerceAtLeast(0)
                                        return true
                                    }
                                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                                        val s = selStart; val e = selEnd
                                        if (s == e) return false
                                        val sel = text.toString().substring(minOf(s, e), maxOf(s, e))
                                        return when (item.itemId) {
                                            MENU_HIGHLIGHT -> {
                                                scope.launch {
                                                    onTextLongPress(sel, tag as? Int ?: -1, selStart, selEnd)
                                                    onAnnotationsClick()
                                                }
                                                mode.finish(); true
                                            }
                                            MENU_NOTE -> {
                                                scope.launch {
                                                    onTextLongPress(sel, tag as? Int ?: -1, selStart, selEnd)
                                                    onNoteClick()
                                                }
                                                mode.finish(); true
                                            }
                                            MENU_TRANSLATE -> {
                                                scope.launch {
                                                    onTextLongPress(sel, tag as? Int ?: -1, selStart, selEnd)
                                                    onTranslateText()
                                                }
                                                mode.finish(); true
                                            }
                                            MENU_DICT -> {
                                                scope.launch {
                                                    onTextLongPress(sel, tag as? Int ?: -1, selStart, selEnd)
                                                    onDictionaryLookup()
                                                }
                                                mode.finish(); true
                                            }
                                            MENU_TTS -> {
                                                onTtsRequest(sel)
                                                mode.finish(); true
                                            }
                                            else -> false
                                        }
                                    }
                                    override fun onDestroyActionMode(mode: ActionMode) {
                                        val s = selStart; val e = selEnd
                                        if (s != e) {
                                            scope.launch {
                                                onTextLongPress(text.toString().substring(minOf(s, e), maxOf(s, e)), tag as? Int ?: -1, s, e)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        update = { tv ->
                            val lineText = displayLines[i]
                            // 解析高亮：locatorJson = "txt_line_{idx}_{start}_{end}"
                            val hlAnns = annotations.filter { it.type == "highlight" }
                            val matched = hlAnns.firstOrNull { ann ->
                                val parts = ann.locatorJson.split("_")
                                parts.size >= 4 && parts[0] == "txt" && parts[1] == "line" && parts[2] == i.toString()
                            }
                            if (matched != null) {
                                val parts = matched.locatorJson.split("_")
                                val start = parts.getOrNull(3)?.toIntOrNull() ?: 0
                                val end = parts.getOrNull(4)?.toIntOrNull() ?: lineText.length
                                val ss = SpannableString(lineText)
                                ss.setSpan(BackgroundColorSpan(matched.color), start.coerceIn(0, lineText.length), end.coerceIn(0, lineText.length), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                tv.text = ss
                            } else {
                                tv.text = lineText
                            }
                            // 搜索关键词高亮
                            if (highlightQuery.isNotBlank() && i == highlightLine) {
                                val ss = if (tv.text is SpannableString) SpannableString(tv.text) else SpannableString(lineText)
                                var pos = 0
                                val lower = lineText.lowercase()
                                val qLower = highlightQuery.lowercase()
                                while (true) {
                                    val idx = lower.indexOf(qLower, pos)
                                    if (idx < 0) break
                                    ss.setSpan(BackgroundColorSpan(android.graphics.Color.parseColor("#FFEB3B")), idx, idx + qLower.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    ss.setSpan(ForegroundColorSpan(android.graphics.Color.RED), idx, idx + qLower.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    pos = idx + qLower.length
                                }
                                tv.text = ss
                            }
                            tv.textSize = currentFontSize.toFloat()
                            tv.setTextColor(textColor)
                            tv.setLineSpacing(0f, lineSpacing)
                            try { tv.typeface = android.graphics.Typeface.create(fontFamily, android.graphics.Typeface.NORMAL) } catch (_: Exception) {}
                            tv.tag = i
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
        }

        // 工具栏覆盖层
        ReaderToolbarOverlay(
            state = ReaderToolbarState(
                title = title,
                currentPage = listState.firstVisibleItemIndex,
                totalPages = displayLines.size,
                darkMode = darkMode,
                fontSize = currentFontSize,
                tocEntries = tocEntries,
                bookmarks = bookmarks,
                isTtsPlaying = isTtsPlaying,
                showStatusBar = showStatusBar,
                showNavBar = showNavBar,
                showProgressBar = showProgressBar,
            ),
            visible = uiVisible,
            onBack = onBack,
            onDarkModeChange = onDarkModeChange,
            onFontSizeChange = { n -> currentFontSize = n; onFontSizeChange(n) },
            onBrightnessChange = onBrightnessChange,
            onOrientationChange = onOrientationChange,
            onPageChange = { scope.launch { listState.animateScrollToItem(it) } },
            onTtsStart = {
                val startIdx = listState.firstVisibleItemIndex
                onTtsRequest(lines.drop(startIdx).take(20).joinToString("\n"))
            },
            onTtsStop = onTtsStop,
            onTocJump = { idx -> scope.launch { listState.animateScrollToItem(idx) } },
            onAddBookmark = { onAddBookmark(listState.firstVisibleItemIndex) },
            onSearch = { query, _ ->
                if (query.isNotBlank()) {
                    searchQuery = query
                    highlightQuery = query
                    searchMatches = displayLines.mapIndexedNotNull { i, line ->
                        if (line.contains(query, ignoreCase = true)) i to line else null
                    }
                    showSearchResults = true
                    showBackToResults = false
                }
            },
            onSearchRequest = {
                // 已有搜索结果时直接打开结果页
                if (searchMatches.isNotEmpty()) {
                    showSearchResults = true
                }
            },
            onReplaceAll = { from, to ->
                val regex = Regex(Regex.escape(from), RegexOption.IGNORE_CASE)
                displayLines = displayLines.map { regex.replace(it, to) }
            },
            book = book,
            sessions = sessions,
            coverUrl = coverUrl,
            onToggleFavorite = onToggleFavorite,
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter,
            onPageTextCopy = onPageTextCopy,
            onBookmarkManage = onBookmarkManage,
            currentVisualOptions = currentVisualOptions,
            currentControlOptions = currentControlOptions,
            currentOtherOptions = currentOtherOptions,
            onSaveVisualOptions = onSaveVisualOptions,
            onSaveControlOptions = onSaveControlOptions,
            onSaveOtherOptions = onSaveOtherOptions,
            nameReplacements = nameReplacements,
            onNameReplaceChange = onNameReplaceChange,
            onSaveSetting = onSaveSetting,
            onSetTranslateEngine = onSetTranslateEngine,
            onSetTranslateTargetLang = onSetTranslateTargetLang,
            onTranslateText = onTranslateText,
            onDictionaryLookup = onDictionaryLookup,
            onSetReaderBgColor = onSetReaderBgColor,
            readerBgColor = readerBgColor,
        )

        // 搜索结果覆盖层
        if (showSearchResults) {
            val bg = if (darkMode) Color(0xFF1A1814) else Color(0xFFF9F7F4)
            val fg = if (darkMode) Color(0xFFE0D8CC) else Color(0xFF1A1A1A)
            Surface(
                color = bg,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Column(Modifier.fillMaxSize()) {
                    // 顶部标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "搜索：${searchQuery}",
                            style = MaterialTheme.typography.titleSmall,
                            color = fg,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "共 ${searchMatches.size} 个结果",
                            style = MaterialTheme.typography.bodySmall,
                            color = fg.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        IconButton(onClick = { showSearchResults = false }) {
                                Icon(Icons.Default.Close, "关闭", tint = fg)
                            }
                    }
                    HorizontalDivider(color = fg.copy(alpha = 0.2f))
                    // 结果列表
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(searchMatches.size) { idx ->
                            val (line, text) = searchMatches[idx]
                            val displayText = text.trim()
                            val spannable = buildSpannableString(displayText, searchQuery)
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        highlightLine = line
                                        listState.animateScrollToItem(line.coerceAtMost(displayLines.size - 1))
                                        showSearchResults = false
                                        showBackToResults = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        "${line + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        spannable,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = fg,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (idx < searchMatches.size - 1) {
                                HorizontalDivider(color = fg.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }

        // 返回搜索结果 FAB
        if (showBackToResults) {
            FloatingActionButton(
                onClick = { showBackToResults = false; showSearchResults = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 100.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.Default.Search,
                    "返回搜索结果",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        if (showTranslation) {
            TranslationDialog(
                originalText = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "",
                translatedText = translatedText,
                isLoading = isTranslating,
                sourceLang = null,
                error = translateError,
                onDismiss = { showTranslation = false },
                engineName = "Google",
            )
        }
    }
}

private fun buildSpannableString(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    if (query.isBlank()) return builder.toAnnotatedString()
    var start = 0
    val lower = text.lowercase()
    val qLower = query.lowercase()
    while (true) {
        val idx = lower.indexOf(qLower, start)
        if (idx < 0) break
        builder.addStyle(
            androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
            idx, idx + qLower.length,
        )
        start = idx + qLower.length
    }
    return builder.toAnnotatedString()
}
