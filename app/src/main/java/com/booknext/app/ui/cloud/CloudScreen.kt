package com.booknext.app.ui.cloud

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedBooks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sortAsc by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }

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
    ) { uri -> uri?.let { uploadViewModel.onFileSelected(context, it) } }

    val isFolderMode = selectedFolder == null
    val hasSelection = selectedBooks.isNotEmpty() || selectedFolders.isNotEmpty()

    val folderNames = remember(state) {
        (state as? CloudUiState.Ready)?.folders?.map { it.name }?.filter { it != "__root__" } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (hasSelection) {
                        Text("已选 ${selectedBooks.size + selectedFolders.size} 项")
                    } else {
                        Text(selectedFolder?.displayName ?: "我的云盘")
                    }
                },
                navigationIcon = {
                    when {
                        hasSelection -> IconButton(onClick = {
                            selectedBooks = emptySet(); selectedFolders = emptySet()
                        }) { Icon(Icons.Default.Close, "取消选择") }
                        selectedFolder != null -> IconButton(onClick = {
                            selectedFolder = null
                        }) { Icon(Icons.Default.ArrowBack, "返回") }
                        else -> IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, "菜单")
                        }
                    }
                },
                actions = {
                    if (hasSelection) {
                        IconButton(onClick = {
                            val s = state as? CloudUiState.Ready ?: return@IconButton
                            val all = s.folders.flatMap { it.books }
                            viewModel.downloadBooks(context, all.filter { it.bookId in selectedBooks }, baseUrl, apiKey)
                        }) {
                            Icon(Icons.Default.Download, "下载")
                        }
                        if (selectedBooks.isNotEmpty() && folderNames.isNotEmpty()) {
                            IconButton(onClick = { showFolderSheet = true }) {
                                Icon(Icons.Default.DriveFileMove, "移动")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "删除",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Default.Upload, "上传")
                        }
                        IconButton(onClick = {
                            val s = state as? CloudUiState.Ready ?: return@IconButton
                            val ids = if (isFolderMode) s.folders.flatMap { it.books }.map { it.bookId }.toSet()
                            else selectedFolder?.books?.map { it.bookId }?.toSet() ?: emptySet()
                            viewModel.downloadBooks(context, s.folders.flatMap { it.books }.filter { it.bookId in ids }, baseUrl, apiKey)
                        }) {
                            Icon(Icons.Default.Download, "下载")
                        }
                        var showCloudMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showCloudMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showCloudMenu,
                            onDismissRequest = { showCloudMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("存储信息") },
                                onClick = { showCloudMenu = false; showStorageInfo = true },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("排序方式") },
                                onClick = {
                                    showCloudMenu = false
                                    sortAsc = !sortAsc
                                    viewModel.load()
                                },
                                leadingIcon = {
                                    Icon(if (sortAsc) Icons.Default.ArrowUpward
                                    else Icons.Default.ArrowDownward, null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("全选") },
                                onClick = {
                                    showCloudMenu = false
                                    if (isFolderMode) {
                                        val s = state as? CloudUiState.Ready
                                        selectedFolders = s?.folders?.map { it.name }?.toSet() ?: emptySet()
                                    } else {
                                        selectedBooks = selectedFolder?.books?.map { it.bookId }?.toSet() ?: emptySet()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                onClick = { showCloudMenu = false; viewModel.load() },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            )
                        }
                    }
                },
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
                        selectedFolders = selectedFolders,
                        onFolderClick = { folder ->
                            if (selectedFolders.isNotEmpty()) {
                                selectedFolders = if (selectedFolders.contains(folder.name))
                                    selectedFolders - folder.name
                                else selectedFolders + folder.name
                            } else {
                                selectedFolder = folder
                            }
                        },
                        onFolderLongClick = { folder ->
                            selectedFolders = selectedFolders + folder.name
                        },
                    )
                } else {
                    CloudBookList(
                        books = selectedFolder!!.books,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        padding = padding,
                        selectedBooks = selectedBooks,
                        onBookClick = { bookId ->
                            if (selectedBooks.isNotEmpty()) {
                                selectedBooks = if (selectedBooks.contains(bookId))
                                    selectedBooks - bookId
                                else selectedBooks + bookId
                            } else {
                                onBookClick(bookId)
                            }
                        },
                        onBookLongClick = { bookId ->
                            selectedBooks = selectedBooks + bookId
                        },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = selectedBooks.size + selectedFolders.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除确认") },
            text = { Text("确认删除选中的 $count 项？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBooks.forEach { viewModel.deleteBook(it) }
                    selectedFolders.forEach { folderName ->
                        val f = (state as? CloudUiState.Ready)?.folders?.find { it.name == folderName }
                        f?.books?.forEach { viewModel.deleteBook(it.bookId) }
                    }
                    selectedBooks = emptySet(); selectedFolders = emptySet()
                    showDeleteConfirm = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
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
                            progress = { (s.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f) },
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

    if (showFolderSheet && selectedBooks.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showFolderSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("移动到分组",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                folderNames.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBooks.forEach { viewModel.moveBook(it, name) }
                                selectedBooks = emptySet()
                                showFolderSheet = false
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.Folder, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudFolderList(
    folders: List<CloudFolder>,
    padding: PaddingValues,
    selectedFolders: Set<String>,
    onFolderClick: (CloudFolder) -> Unit,
    onFolderLongClick: (CloudFolder) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.name }) { folder ->
            val isSelected = selectedFolders.contains(folder.name)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onFolderClick(folder) },
                        onLongClick = { onFolderLongClick(folder) },
                    ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (folder.name == "__root__") Icons.Default.FolderOpen
                        else Icons.Default.Folder, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${folder.books.size} 本 · ${formatSize(folder.books.sumOf { it.fileSize })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudBookList(
    books: List<BookEntity>,
    baseUrl: String,
    apiKey: String,
    padding: PaddingValues,
    selectedBooks: Set<String>,
    onBookClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(books, key = { it.bookId }) { book ->
            val isSelected = selectedBooks.contains(book.bookId)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onBookClick(book.bookId) },
                        onLongClick = { onBookLongClick(book.bookId) },
                    ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier.size(width = 38.dp, height = 52.dp),
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
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(book.format.uppercase(), fontSize = 9.sp)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            book.format.uppercase() + " · " + formatSize(book.fileSize),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
