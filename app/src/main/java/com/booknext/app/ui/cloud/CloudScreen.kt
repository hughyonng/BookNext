package com.booknext.app.ui.cloud

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.ui.bookshelf.PrefsEntryPoint
import com.booknext.app.ui.upload.UploadViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    onMenuClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: CloudViewModel = hiltViewModel(),
    uploadViewModel: UploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedFolder by remember { mutableStateOf<CloudFolder?>(null) }
    var showStorageInfo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, PrefsEntryPoint::class.java
        ).prefs()
    }
    val baseUrl = remember { runBlocking { prefs.serverUrl.first().trimEnd('/') } }
    val apiKey = remember { runBlocking { prefs.apiKey.first() } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadViewModel.onFileSelected(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedFolder?.displayName ?: "我的云盘")
                },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, "菜单")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Default.Upload, "上传")
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    IconButton(onClick = { showStorageInfo = true }) {
                        Icon(Icons.Default.Info, "存储信息")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is CloudUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is CloudUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.msg, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load() }) { Text("重试") }
                    }
                }
            }

            is CloudUiState.Ready -> {
                if (selectedFolder == null) {
                    CloudFolderList(
                        folders = s.folders,
                        padding = padding,
                        onFolderClick = { selectedFolder = it },
                    )
                } else {
                    CloudBookList(
                        books = selectedFolder!!.books,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        padding = padding,
                        onBookClick = onBookClick,
                        onDeleteBook = { viewModel.deleteBook(it) },
                    )
                }
            }
        }
    }

    if (showStorageInfo) {
        val s = state as? CloudUiState.Ready
        AlertDialog(
            onDismissRequest = { showStorageInfo = false },
            title = { Text("存储信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s != null) {
                        val maxBytes = 100L * 1024 * 1024 * 1024
                        LinearProgressIndicator(
                            progress = {
                                (s.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("已用 ${formatSize(s.totalBytes)}")
                            Text("共 100 GB")
                        }
                        Text("共 ${s.bookCount} 本书籍",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    InfoRow("存储位置", "Hugging Face Dataset")
                    InfoRow("数据主权", "归属你的 HF 账号")
                    InfoRow("访问控制", "API Key 鉴权")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageInfo = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun CloudFolderList(
    folders: List<CloudFolder>,
    padding: PaddingValues,
    onFolderClick: (CloudFolder) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.name }) { folder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder) },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (folder.name == "__root__") Icons.Default.FolderOpen
                        else Icons.Default.Folder,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.displayName,
                            style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${folder.books.size} 本 · ${formatSize(folder.books.sumOf { it.fileSize })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CloudBookList(
    books: List<BookEntity>,
    baseUrl: String,
    apiKey: String,
    padding: PaddingValues,
    onBookClick: (String) -> Unit,
    onDeleteBook: (String) -> Unit,
) {
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(books, key = { it.bookId }) { book ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookClick(book.bookId) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier.size(width = 44.dp, height = 60.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                    ) {
                        if (book.hasCover) {
                            AsyncImage(
                                model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center) {
                                Text(book.format.uppercase(), fontSize = 9.sp)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(book.format.uppercase() + " · " + formatSize(book.fileSize),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = { bookToDelete = book }) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("删除书籍") },
            text = { Text("从云端删除《${book.title}》？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteBook(book.bookId)
                    bookToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1024L * 1024 * 1024 ->
        "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    bytes > 1024L * 1024 ->
        "%.1f MB".format(bytes / 1024.0 / 1024.0)
    else -> "%.0f KB".format(bytes / 1024.0)
}
