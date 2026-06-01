package com.booknext.app.ui.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import com.booknext.app.ui.bookshelf.BookshelfViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    onBookClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val recentBooks by viewModel.recentBooks.collectAsState()
    val context = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.booknext.app.ui.bookshelf.PrefsEntryPoint::class.java
        )
    }
    val baseUrl = remember { runBlocking { entryPoint.prefs().serverUrl.first().trimEnd('/') } }
    val apiKey = remember { runBlocking { entryPoint.prefs().apiKey.first() } }

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
        if (recentBooks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recentBooks, key = { it.bookId }) { book ->
                    val dateStr = remember(book.lastReadAt) {
                        if (book.lastReadAt > 0)
                            SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(book.lastReadAt))
                        else ""
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onBookClick(book.bookId) }.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Card(modifier = Modifier.size(width = 56.dp, height = 76.dp),
                            elevation = CardDefaults.cardElevation(2.dp)) {
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
                            Text(book.title, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
                            Text(book.author, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (dateStr.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
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
    }
}
