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
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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

    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, PrefsEntryPoint::class.java).prefs()
    }
    val baseUrl = remember { runBlocking { prefs.serverUrl.first().trimEnd('/') } }
    val apiKey = remember { runBlocking { prefs.apiKey.first() } }

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
                            Icon(Icons.Default.ArrowBack, "返回全部书籍")
                        }
                        isSearching -> IconButton(onClick = {
                            isSearching = false
                            viewModel.onSearch("")
                        }) { Icon(Icons.Default.ArrowBack, "关闭搜索") }
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
                                    LayoutMode.LIST -> Icons.Default.ViewList
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
                                leadingIcon = { Icon(Icons.Default.Sort, null) },
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
                                            selectedBooks = if (isSelected)
                                                selectedBooks - book.bookId
                                            else
                                                selectedBooks + book.bookId
                                        } else {
                                            onBookClick(book)
                                        }
                                    },
                                    onLongClick = {
                                        selectedBooks = selectedBooks + book.bookId
                                    },
                                )
                            }
                        }
                    }

                    if (syncState is SyncState.Loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
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
                        LayoutMode.LIST to Pair(Icons.Default.ViewList, "列表"),
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
                    defaultElevation = if (isSelected) 6.dp else 2.dp
                ),
                shape = MaterialTheme.shapes.small,
                border = if (isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null,
            ) {
                if (book.hasCover) {
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

        if (!compact) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = book.title,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 1.dp),
            )
            if (book.author.isNotEmpty() && book.author != "未知") {
                Text(
                    text = book.author,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 1.dp),
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                text = book.title,
                fontSize = 10.sp,
                maxLines = 2,
                lineHeight = 13.sp,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
