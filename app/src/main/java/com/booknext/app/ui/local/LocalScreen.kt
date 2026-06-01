package com.booknext.app.ui.local

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalScreen(
    onBookClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: LocalViewModel = hiltViewModel(),
) {
    val localBooks by viewModel.localBooks.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地文件") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Default.Add, "导入文件")
                    }
                }
            )
        }
    ) { padding ->
        if (localBooks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("点击右上角 + 导入本地书籍")
                    Text("支持 EPUB / PDF / TXT / MOBI", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(localBooks, key = { it.path }) { book ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBookClick(book.bookId) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Description, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.name, maxLines = 2)
                                Text(book.format.uppercase() + " · " + formatSize(book.size),
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteLocal(book.path) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes > 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
