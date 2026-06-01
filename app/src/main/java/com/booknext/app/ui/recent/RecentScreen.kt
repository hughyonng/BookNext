package com.booknext.app.ui.recent

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.booknext.app.ui.bookshelf.BookshelfViewModel
import com.booknext.app.ui.bookshelf.PrefsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

private const val MAX_SUMMARY_BOOKS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    onBookClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToOnlineLibrary: () -> Unit = {},
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val recentBooks by viewModel.recentBooks.collectAsState()
    val context = LocalContext.current
    var showDetail by remember { mutableStateOf(false) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, PrefsEntryPoint::class.java
        )
    }
    val baseUrl = remember { runBlocking { entryPoint.prefs().serverUrl.first().trimEnd('/') } }
    val apiKey = remember { runBlocking { entryPoint.prefs().apiKey.first() } }

    if (showDetail) {
        RecentDetailPage(
            books = recentBooks,
            baseUrl = baseUrl,
            apiKey = apiKey,
            onBookClick = onBookClick,
            onBack = { showDetail = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("最近阅读") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "菜单")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (recentBooks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("还没有阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("最近阅读", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showDetail = true }) {
                            Text("查看全部")
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                items(recentBooks.take(MAX_SUMMARY_BOOKS), key = { it.bookId }) { book ->
                    val dateStr = remember(book.lastReadAt) {
                        if (book.lastReadAt > 0)
                            SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(book.lastReadAt))
                        else ""
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onBookClick(book.bookId) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Card(
                                modifier = Modifier.size(width = 44.dp, height = 60.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                            ) {
                                if (book.hasCover) {
                                    AsyncImage(
                                        model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                                        contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(book.format.uppercase(), fontSize = 10.sp)
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                Text(book.author, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (dateStr.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(dateStr, fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            FilledTonalButton(onClick = { onBookClick(book.bookId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("继续", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                EntryCard(
                    icon = Icons.Default.Language,
                    title = "网上书库",
                    desc = "免费电子书资源，一键访问",
                    onClick = onNavigateToOnlineLibrary,
                )
            }

            item {
                EntryCard(
                    icon = Icons.Default.Bookmark,
                    title = "书签",
                    desc = "阅读进度标记，快速跳转",
                    onClick = onNavigateToBookmarks,
                )
            }

            item {
                EntryCard(
                    icon = Icons.Default.Edit,
                    title = "摘抄",
                    desc = "标注、笔记和高亮记录",
                    onClick = onNavigateToNotes,
                )
            }

            item {
                EntryCard(
                    icon = Icons.Default.BarChart,
                    title = "阅读统计",
                    desc = "累计阅读时长、已读书籍等数据",
                    onClick = onNavigateToStats,
                )
            }
        }
    }
}

@Composable
private fun EntryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentDetailPage(
    books: List<com.booknext.app.data.local.db.BookEntity>,
    baseUrl: String,
    apiKey: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("最近阅读 — 全部") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(books, key = { it.bookId }) { book ->
                    val dateStr = remember(book.lastReadAt) {
                        if (book.lastReadAt > 0)
                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA).format(Date(book.lastReadAt))
                        else ""
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onBookClick(book.bookId) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Card(
                                modifier = Modifier.size(width = 44.dp, height = 60.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                            ) {
                                if (book.hasCover) {
                                    AsyncImage(
                                        model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                                        contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(book.format.uppercase(), fontSize = 10.sp)
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                Text(book.author, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (dateStr.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(dateStr, fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
