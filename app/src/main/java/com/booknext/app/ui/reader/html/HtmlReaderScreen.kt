package com.booknext.app.ui.reader.html

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.ui.reader.ReaderToolbarOverlay
import com.booknext.app.ui.reader.ReaderToolbarState
import com.booknext.app.util.EncodingDetector
import java.io.File

@Composable
fun HtmlReaderScreen(
    file: File,
    title: String,
    initialPage: Int = 0,
    fontSize: Float = 16f,
    darkMode: Boolean = false,
    onBack: () -> Unit,
    onProgressChange: (Int) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onOrientationChange: (String) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    totalPages: Int = 0,
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
    val context = LocalContext.current
    var uiVisible by remember { mutableStateOf(true) }
    var toolbarState by remember { mutableStateOf(ReaderToolbarState()) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentFontSize by remember { mutableFloatStateOf(fontSize) }
    var currentDarkMode by remember { mutableStateOf(darkMode) }
    var currentPage by remember { mutableIntStateOf(initialPage) }

    val htmlContent = remember(file) {
        EncodingDetector.readText(file)
    }

    fun injectCssAndJs() {
        val wv = webView ?: return
        val bg = if (currentDarkMode) "#1e1e1e" else "#fafafa"
        val fg = if (currentDarkMode) "#e0e0e0" else "#333333"
        val css = """
            body {
                background-color: $bg !important;
                color: $fg !important;
                font-size: ${currentFontSize}px !important;
                line-height: 1.8 !important;
                padding: 16px !important;
                margin: 0 !important;
                font-family: sans-serif !important;
            }
            img { max-width: 100% !important; height: auto; }
            a { color: #4a9eff !important; }
        """.trimIndent()
        wv.evaluateJavascript(
            "(function(){var s=document.getElementById('r-css');if(!s){s=document.createElement('style');s.id='r-css';document.head.appendChild(s)}s.textContent='$css';})()",
            null
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            webView = this@apply
                            injectCssAndJs()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress == 100) {
                                currentPage = 1
                                onProgressChange(1)
                            }
                        }
                    }
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 透明覆盖层，点击切换工具栏显示/隐藏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (uiVisible) Modifier.background(Color.Black.copy(alpha = 0f))
                    else Modifier.background(Color.Black.copy(alpha = 0f))
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { uiVisible = !uiVisible }
                )
        )

        ReaderToolbarOverlay(
            state = ReaderToolbarState(
                title = title,
                currentPage = currentPage,
                totalPages = totalPages,
                darkMode = currentDarkMode,
                fontSize = currentFontSize.toInt(),
                bookmarks = bookmarks,
            ),
            visible = uiVisible,
            onBack = onBack,
            onDarkModeChange = { dm ->
                currentDarkMode = dm; onDarkModeChange(dm); injectCssAndJs()
            },
            onFontSizeChange = { fs ->
                currentFontSize = fs.toFloat(); onFontSizeChange(fs.toFloat()); injectCssAndJs()
            },
            onBrightnessChange = onBrightnessChange,
            onOrientationChange = { o -> onOrientationChange(o) },
            onPageChange = onProgressChange,
            onTtsStart = {},
            onTtsStop = {},
            onTocJump = onProgressChange,
            onAddBookmark = { onAddBookmark(currentPage) },
            onAutoScroll = { _, _ -> },
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
        )
    }
}
