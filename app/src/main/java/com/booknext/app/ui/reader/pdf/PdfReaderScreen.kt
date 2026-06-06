package com.booknext.app.ui.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.ui.reader.ReaderToolbarOverlay
import com.booknext.app.ui.reader.ReaderToolbarState
import com.booknext.app.ui.reader.readerGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private val pdfRenderMutex = Mutex()

@Composable
fun PdfReaderScreen(
    file: File,
    title: String,
    initialPage: Int = 0,
    onBack: () -> Unit = {},
    onProgressChange: (Int) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    onFontSizeChange: (Int) -> Unit = {},
    onOrientationChange: (String) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onToggleUI: () -> Unit = {},
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
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onPageTextCopy: () -> Unit = {},
    onBookmarkManage: () -> Unit = {},
) {
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var totalPages by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scrollMode by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }

    val renderer = remember(file) {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    }
    var pageBitmaps = remember { mutableStateListOf<Bitmap?>() }
    val scope = rememberCoroutineScope()

    DisposableEffect(renderer, scrollMode) {
        totalPages = renderer.pageCount
        if (scrollMode) {
            scope.launch {
                for (i in pageBitmaps.size until renderer.pageCount) {
                    val bmp = withContext(Dispatchers.IO) { renderPage(renderer, i) }
                    pageBitmaps.add(bmp)
                }
            }
        }
        onDispose {
            pageBitmaps.forEach { it?.recycle() }
            renderer.close()
        }
    }

    LaunchedEffect(currentPage, renderer, scrollMode) {
        if (!scrollMode) {
            withContext(Dispatchers.IO) {
                bitmap = renderPage(renderer, currentPage.coerceIn(0, totalPages - 1))
            }
        }
    }

    fun goPage(delta: Int) {
        val next = (currentPage + delta).coerceIn(0, totalPages - 1)
        if (next != currentPage) {
            currentPage = next
            onProgressChange(next)
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (scrollMode) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

            LaunchedEffect(listState.firstVisibleItemIndex) {
                val idx = listState.firstVisibleItemIndex
                if (idx in 0 until totalPages) {
                    onProgressChange(idx)
                }
            }

            LaunchedEffect(listState.layoutInfo.visibleItemsInfo) {
                val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
                val keepRange = ((visibleIndices.minOrNull() ?: 0) - 3)..
                                ((visibleIndices.maxOrNull() ?: 0) + 3)
                for (i in pageBitmaps.indices) {
                    if (i !in keepRange && pageBitmaps[i] != null) {
                        pageBitmaps[i]?.recycle()
                        pageBitmaps[i] = null
                    }
                }
                for (idx in visibleIndices) {
                    while (idx >= pageBitmaps.size) {
                        val i = pageBitmaps.size
                        if (i >= totalPages) break
                        val bmp = withContext(Dispatchers.IO) { renderPage(renderer, i) }
                        pageBitmaps.add(bmp)
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(totalPages) { i ->
                    if (i < pageBitmaps.size && pageBitmaps[i] != null) {
                        Image(
                            bitmap = pageBitmaps[i]!!.asImageBitmap(),
                            contentDescription = "Page ${i + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HorizontalDivider(thickness = 1.dp)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                            Text("${i + 1}")
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
                    .readerGestures(
                        onPrevPage = { goPage(-1) },
                        onNextPage = { goPage(1) },
                        onToggleUI = { uiVisible = !uiVisible },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } ?: CircularProgressIndicator()
            }
        }

        ReaderToolbarOverlay(
            modifier = Modifier.zIndex(1f),
            state = ReaderToolbarState(
                title = title,
                currentPage = currentPage,
                totalPages = totalPages,
                bookmarks = bookmarks,
            ),
            visible = uiVisible,
            onBack = onBack,
            onDarkModeChange = onDarkModeChange,
            onFontSizeChange = onFontSizeChange,
            onBrightnessChange = onBrightnessChange,
            onOrientationChange = onOrientationChange,
            onPageChange = { goPage(it - currentPage) },
            onTtsStart = {},
            onTtsStop = {},
            onTocJump = { goPage(it - currentPage) },
            onAddBookmark = { onAddBookmark(currentPage) },
            onAutoScroll = { _, _ -> scrollMode = true },
            onSearch = { _, _ -> },
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter,
            onPageTextCopy = onPageTextCopy,
            onBookmarkManage = onBookmarkManage,
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

suspend fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
    return pdfRenderMutex.withLock {
        try {
            val page = renderer.openPage(pageIndex)
            val w = 1080
            val h = (w * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        } catch (_: Exception) { null }
    }
}
