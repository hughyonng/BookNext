package com.booknext.app.ui.local

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocalScreen(
    onBookClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    isLoggedIn: Boolean = true,
    onImportToCloud: (Uri) -> Unit = {},
    viewModel: LocalViewModel = hiltViewModel(),
) {
    val localBooks by viewModel.localBooks.collectAsState()
    val context = LocalContext.current
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFile(context, uri)
            if (isLoggedIn) {
                pendingImportUri = uri
                showImportDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedPaths.isNotEmpty()) {
                        Text("已选 ${selectedPaths.size} 项")
                    } else {
                        Text("本地文件")
                    }
                },
                navigationIcon = {
                    if (selectedPaths.isNotEmpty()) {
                        IconButton(onClick = { selectedPaths = emptySet() }) {
                            Icon(Icons.Default.Close, "取消选择")
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, "菜单")
                        }
                    }
                },
                actions = {
                    if (selectedPaths.isNotEmpty()) {
                        IconButton(onClick = {
                            selectedPaths.forEach { path ->
                                val book = localBooks.find { it.path == path } ?: return@forEach
                                viewModel.toggleFavorite(book.bookId)
                            }
                            selectedPaths = emptySet()
                        }) {
                            Icon(Icons.Default.Star, "收藏")
                        }
                        IconButton(onClick = {
                            selectedPaths.forEach { viewModel.deleteLocal(it) }
                            selectedPaths = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, "删除",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Default.Add, "导入文件")
                        }
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
                    val isSelected = selectedPaths.contains(book.path)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        selectedPaths = if (isSelected)
                                            selectedPaths - book.path
                                        else selectedPaths + book.path
                                    } else {
                                        onBookClick(book.bookId)
                                    }
                                },
                                onLongClick = {
                                    selectedPaths = selectedPaths + book.path
                                },
                            ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
    }

    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; pendingImportUri = null },
            title = { Text("导入完成") },
            text = { Text("已保存到本地。是否同时上传到云端？") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri ?: return@TextButton
                    showImportDialog = false
                    pendingImportUri = null
                    onImportToCloud(uri)
                }) { Text("上传到云端") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; pendingImportUri = null }) {
                    Text("仅本地保存")
                }
            },
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes > 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
