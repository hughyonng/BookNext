package com.booknext.app.ui.reader

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.booknext.app.ui.reader.epub.EpubReaderScreen
import com.booknext.app.ui.reader.pdf.PdfReaderScreen
import com.booknext.app.ui.reader.html.HtmlReaderScreen
import com.booknext.app.ui.reader.txt.TxtReaderScreen

@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val book by viewModel.book.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val ttsPlaying by viewModel.ttsPlaying.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val screenOrientation by viewModel.screenOrientation.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val keepScreenOnPref by viewModel.keepScreenOn.collectAsState()
    val blueLightEnabled by viewModel.blueLight.collectAsState()
    val blueLightAmount by viewModel.blueLightAmount.collectAsState()
    val nameReplacements by viewModel.nameReplacements.collectAsState()
    val tapZone by viewModel.tapZone.collectAsState()
    val tapLeftAction by viewModel.tapLeftAction.collectAsState()
    val tapRightAction by viewModel.tapRightAction.collectAsState()
    val tapCenterAction by viewModel.tapCenterAction.collectAsState()
    val nineZoneConfig by viewModel.nineZoneConfig.collectAsState()
    val edgeSwipeBrightnessPref by viewModel.edgeSwipeBrightness.collectAsState()
    val edgeSwipeFontSizePref by viewModel.edgeSwipeFontSize.collectAsState()
    val smartIndentPref by viewModel.smartIndent.collectAsState()
    val removeExtraBlankPref by viewModel.removeExtraBlank.collectAsState()
    val readerVisualOptions by viewModel.visualOptions.collectAsState()
    val readerControlOptions by viewModel.controlOptions.collectAsState()
    val readerOtherOptions by viewModel.otherOptions.collectAsState()
    val readerBgColor by viewModel.bgColor.collectAsState()

    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    var showAnnotationDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var selectedLocator by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteContent by remember { mutableStateOf("") }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var translateResult by remember { mutableStateOf<com.booknext.app.data.remote.TranslationResult?>(null) }
    var translateLoading by remember { mutableStateOf(false) }
    val translateEngine by viewModel.translateEngine.collectAsState()
    val translateTargetLang by viewModel.translateTargetLang.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
        viewModel.loadAnnotations(bookId)
        viewModel.loadSessions(bookId)
    }

    LaunchedEffect(keepScreenOnPref) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            if (keepScreenOnPref)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 状态栏/导航栏显示控制——离开阅读界面时恢复
    val showStatusBar = readerOtherOptions.showStatusBar
    val showNavBar = readerOtherOptions.showNavBar
    DisposableEffect(showStatusBar, showNavBar) {
        val activity = context as? android.app.Activity
        if (activity != null && android.os.Build.VERSION.SDK_INT >= 30) {
            val controller = activity.window.insetsController
            if (controller != null) {
                if (showStatusBar) controller.show(android.view.WindowInsets.Type.statusBars())
                else controller.hide(android.view.WindowInsets.Type.statusBars())
                if (showNavBar) controller.show(android.view.WindowInsets.Type.navigationBars())
                else controller.hide(android.view.WindowInsets.Type.navigationBars())
                if (!showStatusBar || !showNavBar) {
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        onDispose {
            // 退出阅读器时恢复系统状态栏和导航栏
            if (activity != null && android.os.Build.VERSION.SDK_INT >= 30) {
                val ctrl = activity.window.insetsController
                if (ctrl != null) {
                    ctrl.show(android.view.WindowInsets.Type.statusBars())
                    ctrl.show(android.view.WindowInsets.Type.navigationBars())
                    ctrl.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
        }
    }

    if (showSidebar) {
        AnnotationSidebar(
            annotations = annotations,
            onDelete = { viewModel.deleteAnnotation(it) },
            onClose = { showSidebar = false },
        )
    } else {
        when (val s = state) {
            is ReaderState.Idle, is ReaderState.Downloading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        if (s is ReaderState.Downloading) {
                            Text("正在下载书籍…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            is ReaderState.Ready -> {
                val title = book?.title ?: ""
                val activity = context as? android.app.Activity
                val onSaveSetting: (String, Any) -> Unit = { key, value ->
                    when (key) {
                        "showStatusBar" -> if (value is Boolean) viewModel.saveOtherOptions(readerOtherOptions.copy(showStatusBar = value))
                        "showNavBar" -> if (value is Boolean) viewModel.saveOtherOptions(readerOtherOptions.copy(showNavBar = value))
                        "showProgressBar" -> if (value is Boolean) viewModel.saveOtherOptions(readerOtherOptions.copy(showProgressBar = value))
                        "keepScreenOn" -> if (value is Boolean) viewModel.saveOtherOptions(readerOtherOptions.copy(keepScreenOn = value))
                        "screenOrientation" -> if (value is String) { viewModel.setScreenOrientation(value); applyOrientation(activity, value) }
                    }
                }
                Box {
                    when (s.format.lowercase()) {
                        "epub" -> EpubReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            fontSize = fontSize,
                            darkMode = darkMode,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                                viewModel.setBgColor(if (enabled) "#1E2428" else "")
                            },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            onOrientationChange = { viewModel.setScreenOrientation(it) },
                            onBrightnessChange = { viewModel.setBrightness(it) },
                            book = book,
                            sessions = sessions,
                            coverUrl = viewModel.coverUrl,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            currentVisualOptions = readerVisualOptions,
                            currentControlOptions = readerControlOptions,
                            currentOtherOptions = readerOtherOptions,
                            onSaveVisualOptions = { viewModel.saveVisualOptions(it) },
                            onSaveControlOptions = { viewModel.saveControlOptions(it) },
                            onSaveOtherOptions = { viewModel.saveOtherOptions(it) },
                            bookmarks = bookmarks,
                            onAddBookmark = { viewModel.toggleBookmark(it) },
                            onTtsRequest = { viewModel.startTts(it) },
                            isTtsPlaying = ttsPlaying,
                            stopTts = { viewModel.stopTts() },
                            onSaveSetting = onSaveSetting,
                            onSetTranslateEngine = { viewModel.setTranslateEngine(it) },
                            onSetTranslateTargetLang = { viewModel.setTranslateTargetLang(it) },
                            onTranslateText = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要翻译的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    translateResult = null
                                    translateLoading = true
                                    showTranslateDialog = true
                                }
                            },
                            onDictionaryLookup = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要查词的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, selectedText)
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                                    }
                                    val apps = context.packageManager.queryIntentActivities(intent, 0)
                                    if (apps.isEmpty()) {
                                        android.widget.Toast.makeText(context, "未检测到词典应用", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.startActivity(android.content.Intent.createChooser(intent, "选择词典"))
                                    }
                                }
                            },
                            onAnnotationsClick = {
                                if (selectedText.isNotEmpty()) showAnnotationDialog = true
                                else showSidebar = true
                            },
                            onNoteClick = { showNoteDialog = true },
                            onTextLongPress = { text ->
                                selectedText = text
                                selectedLocator = "epub_sel"
                            },
                            onSetReaderBgColor = { viewModel.setBgColor(it) },
                            readerBgColor = readerBgColor,
                            showStatusBar = readerOtherOptions.showStatusBar,
                            showNavBar = readerOtherOptions.showNavBar,
                            showProgressBar = readerOtherOptions.showProgressBar,
                        )
                        "txt", "mobi", "azw3" -> TxtReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            fontSize = fontSize,
                            darkMode = darkMode,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onTtsRequest = { viewModel.startTts(it) },
                            isTtsPlaying = ttsPlaying,
                            onTtsStop = { viewModel.stopTts() },
                            onAnnotationsClick = {
                                if (selectedText.isNotEmpty()) showAnnotationDialog = true
                                else showSidebar = true
                            },
                            onNoteClick = { showNoteDialog = true },
                            onTextLongPress = { text, loc, start, end ->
                                selectedText = text
                                selectedLocator = if (loc >= 0) "txt_line_${loc}_${start}_${end}" else "0"
                            },
                            readerBgColor = readerBgColor,
                            showStatusBar = readerOtherOptions.showStatusBar,
                            showNavBar = readerOtherOptions.showNavBar,
                            showProgressBar = readerOtherOptions.showProgressBar,
                        )
                        "docx", "doc" -> TxtReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            fontSize = fontSize,
                            darkMode = darkMode,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onTtsRequest = { viewModel.startTts(it) },
                            isTtsPlaying = ttsPlaying,
                            onTtsStop = { viewModel.stopTts() },
                            onAnnotationsClick = {
                                if (selectedText.isNotEmpty()) showAnnotationDialog = true
                                else showSidebar = true
                            },
                            onNoteClick = { showNoteDialog = true },
                            onTextLongPress = { text, loc, start, end ->
                                selectedText = text
                                selectedLocator = if (loc >= 0) "txt_line_${loc}_${start}_${end}" else "0"
                            },
                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                                viewModel.setBgColor(if (enabled) "#1E2428" else "")
                            },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            book = book,
                            sessions = sessions,
                            annotations = annotations,
                            coverUrl = viewModel.coverUrl,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            onSaveVisualOptions = { viewModel.saveVisualOptions(it) },
                            onSaveControlOptions = { viewModel.saveControlOptions(it) },
                            onSaveOtherOptions = { viewModel.saveOtherOptions(it) },
                            bookmarks = bookmarks,
                            onAddBookmark = { viewModel.toggleBookmark(it) },
                            nameReplacements = nameReplacements,
                            onNameReplaceChange = { viewModel.setNameReplacements(it) },
                            smartIndent = smartIndentPref,
                            removeExtraBlank = removeExtraBlankPref,
                            fontFamily = fontFamily,
                            lineSpacing = lineSpacing,
                            onOrientationChange = { viewModel.setScreenOrientation(it) },
                            onBrightnessChange = { viewModel.setBrightness(it) },
                            onSaveSetting = onSaveSetting,
                            onSetTranslateEngine = { viewModel.setTranslateEngine(it) },
                            onSetTranslateTargetLang = { viewModel.setTranslateTargetLang(it) },
                            onTranslateText = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要翻译的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    translateResult = null
                                    translateLoading = true
                                    showTranslateDialog = true
                                }
                            },
                            onDictionaryLookup = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要查词的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, selectedText)
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                                    }
                                    val apps = context.packageManager.queryIntentActivities(intent, 0)
                                    if (apps.isEmpty()) {
                                        android.widget.Toast.makeText(context, "未检测到词典应用", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.startActivity(android.content.Intent.createChooser(intent, "选择词典"))
                                    }
                                }
                            },
                            readerBgColor = readerBgColor,
                            showStatusBar = readerOtherOptions.showStatusBar,
                            showNavBar = readerOtherOptions.showNavBar,
                            showProgressBar = readerOtherOptions.showProgressBar,
                        )
                        "md" -> TxtReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            fontSize = fontSize,
                            darkMode = darkMode,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                                viewModel.setBgColor(if (enabled) "#1E2428" else "")
                            },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            book = book,
                            sessions = sessions,
                            coverUrl = viewModel.coverUrl,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            currentVisualOptions = readerVisualOptions,
                            currentControlOptions = readerControlOptions,
                            currentOtherOptions = readerOtherOptions,
                            onSaveVisualOptions = { viewModel.saveVisualOptions(it) },
                            onSaveControlOptions = { viewModel.saveControlOptions(it) },
                            onSaveOtherOptions = { viewModel.saveOtherOptions(it) },
                            bookmarks = bookmarks,
                            onAddBookmark = { viewModel.toggleBookmark(it) },
                            nameReplacements = nameReplacements,
                            onNameReplaceChange = { viewModel.setNameReplacements(it) },
                            smartIndent = smartIndentPref,
                            removeExtraBlank = removeExtraBlankPref,
                            fontFamily = fontFamily,
                            lineSpacing = lineSpacing,
                            onTranslateText = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要翻译的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    translateResult = null
                                    translateLoading = true
                                    showTranslateDialog = true
                                }
                            },
                            onDictionaryLookup = {
                                if (selectedText.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先长按选择要查词的文字", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, selectedText)
                                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                                    }
                                    val apps = context.packageManager.queryIntentActivities(intent, 0)
                                    if (apps.isEmpty()) {
                                        android.widget.Toast.makeText(context, "未检测到词典应用", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.startActivity(android.content.Intent.createChooser(intent, "选择词典"))
                                    }
                                }
                            },
                            readerBgColor = readerBgColor,
                            showStatusBar = readerOtherOptions.showStatusBar,
                            showNavBar = readerOtherOptions.showNavBar,
                            showProgressBar = readerOtherOptions.showProgressBar,
                        )
                        "pdf" -> PdfReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                                viewModel.setBgColor(if (enabled) "#1E2428" else "")
                            },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            onOrientationChange = { viewModel.setScreenOrientation(it) },
                            onBrightnessChange = { viewModel.setBrightness(it) },
                            book = book,
                            sessions = sessions,
                            coverUrl = viewModel.coverUrl,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            bookmarks = bookmarks,
                            onAddBookmark = { viewModel.toggleBookmark(it) },
                            currentVisualOptions = readerVisualOptions,
                            currentControlOptions = readerControlOptions,
                            currentOtherOptions = readerOtherOptions,
                            onSaveVisualOptions = { viewModel.saveVisualOptions(it) },
                            onSaveControlOptions = { viewModel.saveControlOptions(it) },
                            onSaveOtherOptions = { viewModel.saveOtherOptions(it) },
                        )
                        "html", "htm" -> HtmlReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            fontSize = fontSize.toFloat(),
                            darkMode = darkMode,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                                viewModel.setBgColor(if (enabled) "#1E2428" else "")
                            },
                            onFontSizeChange = { viewModel.setFontSize(it.toInt()) },
                            onOrientationChange = { viewModel.setScreenOrientation(it.toString()) },
                            onBrightnessChange = { viewModel.setBrightness(it) },
                            totalPages = 0,
                            book = book,
                            sessions = sessions,
                            coverUrl = viewModel.coverUrl,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            currentVisualOptions = readerVisualOptions,
                            currentControlOptions = readerControlOptions,
                            currentOtherOptions = readerOtherOptions,
                            onSaveVisualOptions = { viewModel.saveVisualOptions(it) },
                            onSaveControlOptions = { viewModel.saveControlOptions(it) },
                            onSaveOtherOptions = { viewModel.saveOtherOptions(it) },
                            bookmarks = bookmarks,
                            onAddBookmark = { viewModel.toggleBookmark(it) },
                        )
                        else -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂不支持 ${s.format} 格式")
                            }
                        }
                    }
                    // green: 蓝光遮罩 overlay
                    if (blueLightEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(1f, 0.7f, 0.4f, (blueLightAmount * 0.2f).coerceIn(0f, 0.4f)))
                        )
                    }
                }
            }

            is ReaderState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(s.msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadBook(bookId) }) { Text("重试") }
                    }
                }
            }
        }
    }

    if (showAnnotationDialog) {
        AnnotationDialog(
            bookId = bookId,
            selectedText = selectedText,
            locatorJson = selectedLocator.toString(),
            onDismiss = { showAnnotationDialog = false },
            onSave = { ann ->
                viewModel.saveAnnotation(ann)
                showAnnotationDialog = false
            },
        )
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false; noteContent = "" },
            title = { Text("笔记") },
            text = {
                Column {
                    Text(selectedText.take(100), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("笔记内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ann = com.booknext.app.data.local.db.AnnotationEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        locatorJson = selectedLocator.toString(),
                        type = "note",
                        selectedText = selectedText,
                        note = noteContent,
                    )
                    viewModel.saveAnnotation(ann)
                    showNoteDialog = false
                    noteContent = ""
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false; noteContent = "" }) { Text("取消") }
            },
        )
    }

    if (showTranslateDialog) {
        val translator = remember { com.booknext.app.data.remote.TranslatorService() }
        LaunchedEffect(showTranslateDialog) {
            if (translateLoading) {
                val result = translator.translate(selectedText, translateTargetLang)
                translateResult = result
                translateLoading = false
            }
        }
        TranslationDialog(
            originalText = selectedText,
            translatedText = translateResult?.takeIf { it.success }?.text,
            isLoading = translateLoading,
            sourceLang = translateResult?.sourceLang,
            error = translateResult?.takeIf { !it.success }?.error,
            engineName = when (translateEngine) { "baidu" -> "百度"; "youdao" -> "有道"; else -> "Google" },
            onDismiss = { showTranslateDialog = false },
            onSwitchEngine = {
                val next = when (translateEngine) { "google" -> "baidu"; "baidu" -> "youdao"; else -> "google" }
                viewModel.setTranslateEngine(next)
            },
        )
    }
}
