package com.booknext.app.ui.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.prefs.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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

    Scaffold(
        topBar = {
            Column {
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
                        } else {
                            Text("我的书架", maxLines = 1)
                        }
                    },
                    navigationIcon = {
                        if (isSearching) {
                            IconButton(onClick = {
                                isSearching = false
                                viewModel.onSearch("")
                            }) {
                                Icon(Icons.Default.ArrowBack, "关闭搜索")
                            }
                        } else {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, "菜单")
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, "搜索")
                            }
                            IconButton(onClick = { viewModel.syncBooks() }) {
                                Icon(Icons.Default.Refresh, "刷新")
                            }
                        }
                    },
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                syncState is SyncState.Loading && books.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                books.isEmpty() -> {
                    Text(
                        if (searchQuery.isNotEmpty()) "未找到匹配的书籍"
                        else "书库为空，请先上传书籍",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(books.size, key = { books[it].bookId }) { i ->
                                BookCard(book = books[i], baseUrl = baseUrl, apiKey = apiKey,
                                    onClick = { onBookClick(books[i]) })
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
}

@Composable
fun BookCard(
    book: BookEntity,
    baseUrl: String,
    apiKey: String,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    Column(
        modifier = (if (compact) Modifier.width(90.dp) else Modifier.fillMaxWidth()).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = (if (compact) Modifier.width(90.dp) else Modifier.fillMaxWidth()).aspectRatio(0.7f),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            if (book.hasCover) {
                AsyncImage(
                    model = "$baseUrl/api/cover/${book.bookId}?k=$apiKey",
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(book.format.uppercase(), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(book.title, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp))
        Text(book.author, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
