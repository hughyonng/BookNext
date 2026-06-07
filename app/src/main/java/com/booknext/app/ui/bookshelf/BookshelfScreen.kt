package com.booknext.app.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.ui.local.LocalViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

enum class SortOrder { TITLE, AUTHOR, UPLOAD_TIME, LAST_READ, ONLINE_ONLY, LOCAL_ONLY }
enum class LayoutMode { GRID_3, GRID_4, LIST }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrefsEntryPoint {
    fun prefs(): UserPreferences
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onBookClick: (BookEntity) -> Unit,
    onMenuClick: () -> Unit,
    onUploadClick: () -> Unit,
    showFavoritesOnly: Boolean = false,
    onFavoritesFilterCleared: () -> Unit = {},
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val metadataState by viewModel.metadataState.collectAsState()

    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, PrefsEntryPoint::class.java).prefs()
    }
    val rawUrl by prefs.serverUrl.collectAsState(initial = "")
    val apiKey by prefs.apiKey.collectAsState(initial = "")
    val baseUrl = remember(rawUrl) { rawUrl.trimEnd('/') }

    var isSearching by remember { mutableStateOf(false) }
    var selectedBooks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.UPLOAD_TIME) }
    var layoutMode by remember { mutableStateOf(LayoutMode.GRID_3) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var showMetadataDialog by remember { mutableStateOf(false) }
    var metadataApiKey by remember { mutableStateOf("") }

    var coverTargetBookId by remember { mutableStateOf<String?>(null) }
    var showCoverPickerDialog by remember { mutableStateOf(false) }
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val bookId = coverTargetBookId ?: return@rememberLauncherForActivityResult
        if (uri != null) viewModel.saveCoverFromUri(bookId, uri)
        coverTargetBookId = null
    }

    val folders by viewModel.folders.collectAsState()
    val localViewModel: LocalViewModel = hiltViewModel()
    val importFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { localViewModel.importFile(context, it) } }

    val sortedBooks = remember(books, sortOrder, showFavoritesOnly) {
        val filtered = when {
            showFavoritesOnly -> books.filter { it.isFavorite }
            sortOrder == SortOrder.ONLINE_ONLY -> books.filter { it.filePath == null }
            sortOrder == SortOrder.LOCAL_ONLY -> books.filter { it.filePath != null }
            else -> books
        }
        when (sortOrder) {
            SortOrder.TITLE -> filtered.sortedBy { it.title }
            SortOrder.AUTHOR -> filtered.sortedBy { it.author }
            SortOrder.UPLOAD_TIME -> filtered.sortedByDescending { it.uploadTime }
            SortOrder.LAST_READ -> filtered.sortedByDescending { it.lastReadAt }
            SortOrder.ONLINE_ONLY -> filtered.sortedByDescending { it.uploadTime }
            SortOrder.LOCAL_ONLY -> filtered.sortedByDescending { it.uploadTime }
        }
    }

    val columns = when (layoutMode) {
        LayoutMode.GRID_3 -> GridCells.Fixed(3)
        LayoutMode.GRID_4 -> GridCells.Fixed(4)
        LayoutMode.LIST -> GridCells.Fixed(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearch(it) },
                            placeholder = { Text("搜索书名、作者…") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                            ),
                        )
                    } else if (showFavoritesOnly) {
                        Text("我的收藏", maxLines = 1)
                    } else if (selectedBooks.isNotEmpty()) {
                        Text("已选 ${selectedBooks.size} 本")
                    } else {
                        Text("我的书架", maxLines = 1)
                    }
                },
                navigationIcon = {
                    when {
                        showFavoritesOnly -> IconButton(onClick = onFavoritesFilterCleared) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回全部书籍")
                        }
                        isSearching -> IconButton(onClick = {
                            isSearching = false
                            viewModel.onSearch("")
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "关闭搜索") }
                        selectedBooks.isNotEmpty() -> IconButton(onClick = {
                            selectedBooks = emptySet()
                        }) { Icon(Icons.Default.Close, "取消选择") }
                        else -> IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, "菜单")
                        }
                    }
                },
                actions = {
                    if (selectedBooks.isNotEmpty()) {
                        IconButton(onClick = {
                            selectedBooks.forEach { viewModel.toggleFavorite(it) }
                        }) {
                            Icon(Icons.Default.Star, "收藏")
                        }
                        IconButton(onClick = { showFolderSheet = true }) {
                            Icon(Icons.Default.FolderOpen, "添加到分组")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "删除",
                                tint = MaterialTheme.colorScheme.error)
                        }
                        var showMultiMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMultiMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showMultiMenu,
                            onDismissRequest = { showMultiMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("书籍信息") },
                                onClick = { showMultiMenu = false; showInfoDialog = true },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                            )
                            if (selectedBooks.size == 1) {
                                DropdownMenuItem(
                                    text = { Text("设置封面") },
                                    onClick = {
                                        showMultiMenu = false
                                        coverTargetBookId = selectedBooks.first()
                                        showCoverPickerDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, null) },
                                )
                            }
                        }
                    } else if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, "搜索")
                        }
                        IconButton(onClick = { showLayoutPicker = true }) {
                            Icon(
                                when (layoutMode) {
                                    LayoutMode.GRID_3 -> Icons.Default.GridView
                                    LayoutMode.GRID_4 -> Icons.Default.Apps
                                    LayoutMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                                },
                                "布局",
                            )
                        }
                        IconButton(onClick = onUploadClick) {
                            Icon(Icons.Default.Add, "上传书籍")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("排序方式") },
                                onClick = { showMenu = false; showSortSheet = true },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("刷新书库") },
                                onClick = { showMenu = false; viewModel.syncBooks() },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("导入本地书籍") },
                                onClick = {
                                    showMenu = false
                                    importFilePicker.launch("*/*")
                                },
                                leadingIcon = { Icon(Icons.Default.FileOpen, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("全选") },
                                onClick = {
                                    showMenu = false
                                    selectedBooks = books.map { it.bookId }.toSet()
                                },
                                leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("补全书籍信息") },
                                onClick = {
                                    showMenu = false
                                    showMetadataDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, null) },
                            )
                        }
                    }
                },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                syncState is SyncState.Loading && books.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                sortedBooks.isEmpty() -> {
                    Text(
                        if (showFavoritesOnly) "还没有收藏的书籍"
                        else if (searchQuery.isNotEmpty()) "未找到匹配的书籍"
                        else "书库为空，请先上传书籍",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (layoutMode == LayoutMode.LIST) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val list = sortedBooks
                                items(list.size) { index ->
                                    val book = list[index]
                                    val isSelected = selectedBooks.contains(book.bookId)
                                    ListBookRow(
                                        book = book,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (selectedBooks.isNotEmpty()) {
                                                selectedBooks = if (isSelected) selectedBooks - book.bookId
                                                else selectedBooks + book.bookId
                                            } else onBookClick(book)
                                        },
                                        onLongClick = { selectedBooks = selectedBooks + book.bookId },
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = columns,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(sortedBooks, key = { it.bookId }) { book ->
                                    val isSelected = selectedBooks.contains(book.bookId)
                                    BookCard(
                                        book = book,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (selectedBooks.isNotEmpty()) {
                                                selectedBooks = if (isSelected) selectedBooks - book.bookId
                                                else selectedBooks + book.bookId
                                            } else onBookClick(book)
                                        },
                                        onLongClick = { selectedBooks = selectedBooks + book.bookId },
                                    )
                                }
                            }
                        }
                    }

                    if (syncState is SyncState.Loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }

            if (metadataState is MetadataState.Running) {
                val m = metadataState as MetadataState.Running
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(2.dp))
                    Text("补全中：${m.current}/${m.total}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (syncState is SyncState.Error) {
                val msg = (syncState as SyncState.Error).msg
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.syncBooks() }) { Text("重试") } },
                ) { Text("同步失败：$msg") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除书籍") },
            text = { Text("确认删除选中的 ${selectedBooks.size} 本书籍？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBooks.forEach { viewModel.deleteBook(it) }
                    selectedBooks = emptySet()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showInfoDialog && selectedBooks.size == 1) {
        val book = books.find { it.bookId == selectedBooks.first() }
        if (book != null) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text(book.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("书号", book.bookId)
                        InfoRow("作者", book.author)
                        InfoRow("格式", book.format.uppercase())
                        InfoRow("大小", formatSize(book.fileSize))
                        InfoRow("分类", book.category.ifEmpty { "未分类" })
                        InfoRow("进度", if (book.readingPercent > 0) "${(book.readingPercent * 100).toInt()}%" else "—")
                        InfoRow("收藏", if (book.isFavorite) "是" else "否")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }

    if (showFolderSheet && selectedBooks.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showFolderSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("添加到分组",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                if (folders.isEmpty()) {
                    Text("暂无分组，请先在分类页创建",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBooks.forEach { viewModel.addBookToFolder(it, folder) }
                                    selectedBooks = emptySet()
                                    showFolderSheet = false
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Folder, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text(folder, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("排序方式", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                Text("排序", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
                listOf(
                    SortOrder.UPLOAD_TIME to "上传时间",
                    SortOrder.LAST_READ to "最近阅读",
                    SortOrder.TITLE to "书名",
                    SortOrder.AUTHOR to "作者",
                ).forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = order; showSortSheet = false }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (sortOrder == order) {
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Text("筛选", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
                listOf(
                    SortOrder.ONLINE_ONLY to "仅在线",
                    SortOrder.LOCAL_ONLY to "仅本地",
                ).forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = order; showSortSheet = false }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (sortOrder == order) {
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLayoutPicker) {
        ModalBottomSheet(onDismissRequest = { showLayoutPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("显示布局", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(
                        LayoutMode.LIST to Pair(Icons.AutoMirrored.Filled.ViewList, "列表"),
                        LayoutMode.GRID_3 to Pair(Icons.Default.GridView, "3列"),
                        LayoutMode.GRID_4 to Pair(Icons.Default.Apps, "4列"),
                    ).forEach { (mode, pair) ->
                        val (icon, label) = pair
                        val selected = layoutMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { layoutMode = mode; showLayoutPicker = false }
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(icon, null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(label, fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showCoverPickerDialog) {
        val book = coverTargetBookId?.let { id -> books.find { it.bookId == id } }
        AlertDialog(
            onDismissRequest = { showCoverPickerDialog = false; coverTargetBookId = null },
            title = { Text("设置封面") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(book?.title ?: "", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            showCoverPickerDialog = false
                            coverPicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("从相册选择")
                    }
                    if (book?.filePath != null) {
                        TextButton(
                            onClick = {
                                showCoverPickerDialog = false
                                val id = coverTargetBookId ?: return@TextButton
                                viewModel.extractCoverFromFile(id, book.filePath!!, book.format)
                                coverTargetBookId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("从文件提取封面")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverPickerDialog = false; coverTargetBookId = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showMetadataDialog) {
        val isRunning = metadataState is MetadataState.Running
        val savedKey = prefs.googleBooksApiKey.collectAsState(initial = "")
        LaunchedEffect(showMetadataDialog) { metadataApiKey = savedKey.value }

        AlertDialog(
            onDismissRequest = { if (!isRunning) { showMetadataDialog = false; viewModel.resetMetadataState() } },
            title = { Text("补全书籍信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("从 Google Books API 自动补全作者、封面等信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (metadataState is MetadataState.Idle || metadataState is MetadataState.Done) {
                        OutlinedTextField(
                            value = metadataApiKey,
                            onValueChange = { metadataApiKey = it },
                            label = { Text("Google Books API 密钥") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                        )
                    }
                    when (val ms = metadataState) {
                        is MetadataState.Running -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("补全中 ${ms.current}/${ms.total}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is MetadataState.Done -> {
                            Text("✅ 已完成，共更新 ${ms.updated} 本书籍信息",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                when (metadataState) {
                    is MetadataState.Idle -> TextButton(onClick = {
                        val key = metadataApiKey.trim()
                        if (key.isNotEmpty()) {
                            viewModel.autoFillMetadata(key)
                        }
                    }, enabled = metadataApiKey.trim().isNotEmpty()) { Text("开始补全") }
                    is MetadataState.Running -> {}
                    is MetadataState.Done -> TextButton(onClick = {
                        showMetadataDialog = false; viewModel.resetMetadataState()
                    }) { Text("完成") }
                    else -> {}
                }
            },
            dismissButton = {
                if (metadataState !is MetadataState.Running) {
                    TextButton(onClick = { showMetadataDialog = false; viewModel.resetMetadataState() }) { Text("取消") }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookEntity,
    baseUrl: String,
    apiKey: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    compact: Boolean = false,
) {
    Column(
        modifier = (if (compact) Modifier.width(72.dp) else Modifier.fillMaxWidth()).combinedClickable(
            onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Box {
            Card(
                modifier = if (compact)
                    Modifier.size(width = 72.dp, height = 98.dp)
                else
                    Modifier.fillMaxWidth().aspectRatio(0.68f),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 8.dp else 3.dp
                ),
                shape = RoundedCornerShape(10.dp),
                border = if (isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null,
            ) {
                if (book.coverPath != null) {
                    AsyncImage(
                        model = File(book.coverPath),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (book.hasCover) {
                    AsyncImage(
                        model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val bgColor = when (book.format.lowercase()) {
                        "epub" -> Color(0xFF1565C0)
                        "pdf" -> Color(0xFFC62828)
                        "txt" -> Color(0xFF2E7D32)
                        "mobi", "azw3" -> Color(0xFF6A1B9A)
                        else -> Color(0xFF37474F)
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                book.format.uppercase(),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = if (compact) 9.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                book.title.take(1),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = if (compact) 18.sp else 28.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check, null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListBookRow(
    book: BookEntity,
    baseUrl: String,
    apiKey: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.size(width = 44.dp, height = 60.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(6.dp),
        ) {
            if (book.coverPath != null) {
                AsyncImage(model = File(book.coverPath), contentDescription = book.title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (book.hasCover) {
                AsyncImage(model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                    contentDescription = book.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else {
                val bgColor = when (book.format.lowercase()) {
                    "epub" -> Color(0xFF1565C0); "pdf" -> Color(0xFFC62828)
                    "txt" -> Color(0xFF2E7D32); "mobi", "azw3" -> Color(0xFF6A1B9A)
                    else -> Color(0xFF37474F)
                }
                Box(Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
                    Text(book.format.uppercase(), color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val author = if (book.author.isNotEmpty() && book.author != "未知") book.author else ""
                if (author.isNotEmpty()) {
                    Text(author, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (book.readingPercent > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { book.readingPercent },
                        modifier = Modifier.height(3.dp).width(60.dp),
                    )
                    Text("${(book.readingPercent * 100).toInt()}%",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes > 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label：", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, maxLines = 1)
    }
}
