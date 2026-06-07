package com.booknext.app.ui.cloud

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.ui.bookshelf.PrefsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CloudScreen(
    onMenuClick: () -> Unit,
    onBookClick: (String) -> Unit,
    isLoggedIn: Boolean = true,
    pendingUploadUri: android.net.Uri? = null,
    viewModel: CloudViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    var selectedFolder by remember { mutableStateOf<CloudFolder?>(null) }
    var showStorageInfo by remember { mutableStateOf(false) }
    var selectedBooks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sortAsc by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }

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
        if (uri != null) viewModel.uploadFile(context, uri)
    }

    val isFolderMode = selectedFolder == null
    val hasSelection = selectedBooks.isNotEmpty() || selectedFolders.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()

    val folderNames = remember(state) {
        (state as? CloudUiState.Ready)?.folders?.map { it.name }?.filter { it != "__root__" } ?: emptyList()
    }

    val runningTransfers = remember(transfers) { transfers.filter { it.status == TransferStatus.RUNNING } }
    val hasRunning = runningTransfers.isNotEmpty()

    // 来自本地导入的待上传文件
    LaunchedEffect(pendingUploadUri) {
        if (pendingUploadUri != null && isLoggedIn) {
            viewModel.uploadFile(context, pendingUploadUri)
        }
    }

    if (!isLoggedIn) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("我的云盘") },
                    navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "菜单") } },
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("请先登录以访问云盘", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
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
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
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
                            val books = all.filter { it.bookId in selectedBooks }
                            viewModel.downloadBooks(context, books, baseUrl, apiKey)
                            snackScope.launch { snackbarHostState.showSnackbar("已加入传输队列 (${books.size} 项)") }
                        }) {
                            Icon(Icons.Default.Download, "下载")
                        }
                        if (selectedBooks.isNotEmpty() && folderNames.isNotEmpty()) {
                            IconButton(onClick = { showFolderSheet = true }) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, "移动")
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
                            val books = s.folders.flatMap { it.books }.filter { it.bookId in ids }
                            viewModel.downloadBooks(context, books, baseUrl, apiKey)
                            snackScope.launch { snackbarHostState.showSnackbar("已加入传输队列 (${books.size} 项)") }
                        }) {
                            Icon(Icons.Default.Download, "下载")
                        }
                        var showCloudMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showTransferSheet = true }) {
                            Box {
                                Icon(Icons.Default.Sync, "传输记录")
                                if (transfers.any { it.status == TransferStatus.RUNNING }) {
                                    Badge(modifier = Modifier.align(Alignment.TopEnd).size(8.dp))
                                }
                            }
                        }
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
                                onClick = { showCloudMenu = false; sortAsc = !sortAsc; viewModel.load() },
                                leadingIcon = { Icon(if (sortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null) },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is CloudUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CloudUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    }

    // ── 传输记录全屏页 ──
    if (showTransferSheet) {
        CloudTransferPage(
            transfers = transfers,
            onBack = { showTransferSheet = false },
            onClearCompleted = { viewModel.clearCompletedTransfers() },
            onOpenBook = { bookId -> onBookClick(bookId) },
            onRetry = { item -> viewModel.retryTransfer(context, item, baseUrl, apiKey) },
        )
        return
    }

    // ── 其他对话框 ──
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
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } },
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
                        LinearProgressIndicator(progress = { (s.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("已用 ${formatSize(s.totalBytes)}")
                            Text("共 100 GB")
                        }
                        Text("共 ${s.bookCount} 本书籍", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    InfoRow("存储位置", "Hugging Face Dataset")
                    InfoRow("数据主权", "归属你的 HF 账号")
                    InfoRow("访问控制", "API Key 鉴权")
                }
            },
            confirmButton = { TextButton(onClick = { showStorageInfo = false }) { Text("关闭") } },
        )
    }

    if (showFolderSheet && selectedBooks.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showFolderSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("移动到分组", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                folderNames.forEach { name ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedBooks.forEach { viewModel.moveBook(it, name) }
                            selectedBooks = emptySet(); showFolderSheet = false
                        }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudTransferPage(
    transfers: List<TransferItem>,
    onBack: () -> Unit,
    onClearCompleted: () -> Unit,
    onOpenBook: (String) -> Unit,
    onRetry: (TransferItem) -> Unit,
) {
    val running = transfers.filter { it.status == TransferStatus.RUNNING }
    val completed = transfers.filter { it.status != TransferStatus.RUNNING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("传输记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (completed.isNotEmpty()) {
                        TextButton(onClick = onClearCompleted) {
                            Text("清除已完成", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(15.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (transfers.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("暂无传输记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@LazyColumn
            }

            if (running.isNotEmpty()) {
                item {
                    Text("进行中", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(running, key = { it.id }) { t ->
                    TransferItemCard(t = t, onClick = null, onRetry = null)
                }
            }

            if (completed.isNotEmpty()) {
                item {
                    Text("已完成", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (running.isNotEmpty()) 16.dp else 8.dp, bottom = 4.dp))
                }
                items(completed, key = { it.id }) { t ->
                    TransferItemCard(
                        t = t,
                        onClick = if (t.status == TransferStatus.SUCCESS && t.bookId != null) {
                            { onOpenBook(t.bookId!!) }
                        } else null,
                        onRetry = if (t.status == TransferStatus.ERROR) {
                            { onRetry(t) }
                        } else null,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TransferItemCard(
    t: TransferItem,
    onClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val progress = if (t.totalBytes > 0) (t.transferredBytes.toFloat() / t.totalBytes).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 3.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                when (t.type) {
                    TransferType.UPLOAD -> Icons.Default.Upload
                    TransferType.DOWNLOAD -> Icons.Default.Download
                }, null,
                tint = when (t.status) {
                    TransferStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    TransferStatus.SUCCESS -> Color(0xFF4CAF50)
                    TransferStatus.ERROR -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(t.fileName, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                when (t.status) {
                    TransferStatus.RUNNING -> {
                        if (t.transferredBytes > 0) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                            Spacer(Modifier.height(3.dp))
                            Text("${(progress * 100).toInt()}% · ${formatSize(t.transferredBytes)}/${formatSize(t.totalBytes)}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            // 初始状态：等待中
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                            Spacer(Modifier.height(3.dp))
                            Text("等待传输…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TransferStatus.SUCCESS -> {
                        Text(
                            if (t.bookId != null) "传输完成 · 点击阅读" else "传输完成",
                            fontSize = 12.sp, color = Color(0xFF4CAF50),
                        )
                    }
                    TransferStatus.ERROR -> {
                        Text("失败：${t.errorMessage ?: "未知错误"}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // 右侧操作按钮
            if (t.status == TransferStatus.RUNNING) {
                if (t.transferredBytes > 0) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, progress = { progress })
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else if (t.status == TransferStatus.ERROR && onRetry != null) {
                IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "重试", tint = MaterialTheme.colorScheme.primary)
                }
            } else if (t.status == TransferStatus.SUCCESS && onClick != null) {
                IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudFolderList(
    folders: List<CloudFolder>,
    selectedFolders: Set<String>,
    onFolderClick: (CloudFolder) -> Unit,
    onFolderLongClick: (CloudFolder) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.name }) { folder ->
            val isSelected = selectedFolders.contains(folder.name)
            Card(
                modifier = Modifier.fillMaxWidth().combinedClickable(
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) },
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(if (folder.name == "__root__") Icons.Default.FolderOpen else Icons.Default.Folder, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(folder.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text("${folder.books.size} 本 · ${formatSize(folder.books.sumOf { it.fileSize })}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    else Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    selectedBooks: Set<String>,
    onBookClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(books, key = { it.bookId }) { book ->
            val isSelected = selectedBooks.contains(book.bookId)
            Card(
                modifier = Modifier.fillMaxWidth().combinedClickable(
                    onClick = { onBookClick(book.bookId) },
                    onLongClick = { onBookLongClick(book.bookId) },
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(modifier = Modifier.size(width = 38.dp, height = 52.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        if (book.hasCover) {
                            AsyncImage(model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(book.format.uppercase(), fontSize = 9.sp)
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(book.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                        Text(book.format.uppercase() + " · " + formatSize(book.fileSize),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
    bytes > 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    bytes > 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    else -> "%.0f KB".format(bytes / 1024.0)
}
