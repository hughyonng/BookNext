package com.booknext.app.ui.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    file: File,
    title: String,
    initialPage: Int,
    onBack: () -> Unit,
    onProgressChange: (Int) -> Unit,
) {
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var totalPages by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var scrollMode by remember { mutableStateOf(false) }

    val renderer = remember(file) {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    }
    var pageBitmaps = remember { mutableStateListOf<Bitmap?>() }
    val scope = rememberCoroutineScope()

    DisposableEffect(renderer) {
        totalPages = renderer.pageCount
        // pre-render all pages for scroll mode
        if (scrollMode) {
            scope.launch(Dispatchers.IO) {
                for (i in pageBitmaps.size until renderer.pageCount) {
                    val bmp = renderPage(renderer, i)
                    pageBitmaps.add(bmp)
                }
            }
        }
        onDispose { renderer.close() }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { scrollMode = !scrollMode }) {
                        Text(if (scrollMode) "单页" else "滚动", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            if (!scrollMode) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { goPage(-1) }, enabled = currentPage > 0) { Text("◀") }
                        Text("${currentPage + 1} / $totalPages")
                        IconButton(onClick = { goPage(1) }, enabled = currentPage < totalPages - 1) { Text("▶") }
                    }
                }
            }
        }
    ) { padding ->
        if (scrollMode) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

            LaunchedEffect(listState.firstVisibleItemIndex) {
                val idx = listState.firstVisibleItemIndex
                if (idx in 0 until totalPages) {
                    onProgressChange(idx)
                }
            }

            // Lazy pre-render
            LaunchedEffect(listState.layoutInfo.visibleItemsInfo) {
                scope.launch(Dispatchers.IO) {
                    val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
                    for (idx in visibleIndices) {
                        while (idx >= pageBitmaps.size) {
                            // Fill in gaps
                            val i = pageBitmaps.size
                            if (i < totalPages) {
                                val bmp = renderPage(renderer, i)
                                pageBitmaps.add(bmp)
                            } else break
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
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
                modifier = Modifier.fillMaxSize().padding(padding)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragDelta = 0f },
                            onHorizontalDrag = { _, delta -> dragDelta += delta },
                            onDragEnd = {
                                if (dragDelta < -80f) goPage(1)
                                else if (dragDelta > 80f) goPage(-1)
                                dragDelta = 0f
                            }
                        )
                    },
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
    }
}

fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
    return try {
        val page = renderer.openPage(pageIndex)
        val w = 1080
        val h = (w * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bmp
    } catch (_: Exception) { null }
}