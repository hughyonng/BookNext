package com.booknext.app.ui.reader.txt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.booknext.app.ui.reader.readerGestures
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onTextLongPress: (String, Int) -> Unit = { _, _ -> },
) {
    val lines = remember(file) { file.readLines(Charsets.UTF_8) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val bgColor = if (darkMode) Color(0xFF1A1814) else Color(0xFFF9F7F4)
    val textColor = if (darkMode) Color(0xFFE0D8CC) else Color(0xFF1A1A1A)
    var uiVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex > 0) {
            onProgressChange(listState.firstVisibleItemIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize().readerGestures(
            onPrevPage = { scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex - 16).coerceAtLeast(0)) } },
            onNextPage = { scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex + 16).coerceAtMost(lines.size - 1)) } },
            onToggleUI = { uiVisible = !uiVisible },
        )) {
        Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = uiVisible) {
                    TopAppBar(
                        title = { Text(title, maxLines = 1) },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                        actions = { IconButton(onClick = onAnnotationsClick) { Icon(Icons.Default.Bookmark, "标注") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor, titleContentColor = textColor),
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(lines.size) { i ->
                        Text(
                            text = lines[i],
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.8f).sp,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 2.dp)
                                .combinedClickable(onClick = {}, onLongClick = { onTextLongPress(lines[i], i) }),
                        )
                    }
                }
                AnimatedVisibility(visible = uiVisible) {
                    BottomAppBar(containerColor = bgColor) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = if (isTtsPlaying) Arrangement.SpaceBetween else Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isTtsPlaying) {
                                Text("朗读中…", color = textColor)
                                Button(onClick = onTtsStop) { Text("停止") }
                            } else {
                                TextButton(onClick = {
                                    val startIdx = listState.firstVisibleItemIndex
                                    onTtsRequest(lines.drop(startIdx).take(20).joinToString("\n"))
                                }) { Text("朗读此处", color = textColor) }
                            }
                        }
                    }
                }
            }
        }
    }
}