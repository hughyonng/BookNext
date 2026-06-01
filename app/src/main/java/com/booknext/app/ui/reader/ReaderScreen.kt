package com.booknext.app.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.booknext.app.ui.reader.epub.EpubReaderScreen
import com.booknext.app.ui.reader.pdf.PdfReaderScreen
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

    var showAnnotationDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var selectedLocator by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
        viewModel.loadAnnotations(bookId)
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
                        )
                        "pdf" -> PdfReaderScreen(
                            file = s.file,
                            title = title,
                            initialPage = progress.toIntOrNull() ?: 0,
                            onBack = onBack,
                            onProgressChange = { viewModel.savePageProgress(it) },
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
                            onAnnotationsClick = { showSidebar = true },
                            onTextLongPress = { text, loc ->
                                selectedText = text
                                selectedLocator = "0"
                                showAnnotationDialog = true
                            },
                        )
                        else -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂不支持 ${s.format} 格式")
                            }
                        }
                    }
                }
            }

            is ReaderState.Error -> {
                val isUnsupported = book?.format in listOf("mobi", "azw3")
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(s.msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        if (!isUnsupported) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadBook(bookId) }) { Text("重试") }
                        }
                    }
                }
            }
        }
    }

    // 标注弹窗
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
}