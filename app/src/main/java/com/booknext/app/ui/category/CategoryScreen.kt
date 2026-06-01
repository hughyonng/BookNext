package com.booknext.app.ui.category

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import com.booknext.app.ui.bookshelf.BookCard
import com.booknext.app.ui.bookshelf.BookshelfViewModel
import com.booknext.app.ui.bookshelf.PrefsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    onBookClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsState()
    val allBooks by viewModel.books.collectAsState()
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddBookSheet by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var selectedFolderForEdit by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, PrefsEntryPoint::class.java
        ).prefs()
    }
    val baseUrl = remember { runBlocking { prefs.serverUrl.first().trimEnd('/') } }
    val apiKey = remember { runBlocking { prefs.apiKey.first() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedFolderForEdit != null) Text("已选中 1 项")
                    else Text(selectedFolder ?: "我的分类")
                },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    } else if (selectedFolderForEdit != null) {
                        IconButton(onClick = { selectedFolderForEdit = null }) {
                            Icon(Icons.Default.Close, "取消选中")
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, "菜单")
                        }
                    }
                },
                actions = {
                    if (selectedFolderForEdit != null) {
                        IconButton(onClick = {
                            folderToDelete = selectedFolderForEdit
                            showDeleteConfirm = true
                        }) {
                            Icon(Icons.Default.Delete, "删除文件夹",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (selectedFolder == null) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "新建文件夹")
                        }
                    } else {
                        IconButton(onClick = { showAddBookSheet = true }) {
                            Icon(Icons.Default.Add, "添加书籍")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedFolder == null) {
            if (folders.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("还没有分类")
                        Text("点击右上角新建文件夹", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(folders) { folder ->
                        val count = allBooks.count { it.category == folder }
                        val isSelected = folder == selectedFolderForEdit

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isSelected) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { selectedFolder = folder },
                                        onLongClick = {
                                            selectedFolderForEdit = if (isSelected) null else folder
                                        },
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.Folder,
                                    null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder, style = MaterialTheme.typography.bodyLarge)
                                    Text("$count 本书",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            val booksInFolder = allBooks.filter { it.category == selectedFolder }
            if (booksInFolder.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LibraryAdd, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("文件夹为空")
                        Text("点击右上角 + 添加书籍", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(booksInFolder, key = { it.bookId }) { book ->
                        BookCard(
                            book = book,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            onClick = { onBookClick(book.bookId) },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && folderToDelete != null) {
        val f = folderToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; folderToDelete = null; selectedFolderForEdit = null },
            title = { Text("删除文件夹") },
            text = { Text("删除「$f」？文件夹内的书籍将变为未分类，书籍本身不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(f)
                    showDeleteConfirm = false
                    folderToDelete = null
                    selectedFolderForEdit = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; folderToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newFolderName = "" },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            showCreateDialog = false
                            newFolderName = ""
                        }
                    },
                    enabled = newFolderName.isNotBlank(),
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newFolderName = "" }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAddBookSheet && selectedFolder != null) {
        val folder = selectedFolder!!
        val availableBooks = allBooks.filter { it.category != folder }

        ModalBottomSheet(
            onDismissRequest = { showAddBookSheet = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("添加书籍到「$folder」",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                if (availableBooks.isEmpty()) {
                    Text("所有书籍都已在此文件夹中",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(availableBooks, key = { it.bookId }) { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addBookToFolder(book.bookId, folder)
                                        showAddBookSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Book, null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, maxLines = 1)
                                    Text(book.author, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1)
                                }
                                Icon(Icons.Default.Add, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
