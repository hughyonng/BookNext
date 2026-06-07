package com.booknext.app.ui.onlinelibrary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlineLibraryScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: OnlineLibraryViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val hasSelection = selectedIndex != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网上书库") },
                navigationIcon = {
                    if (hasSelection) {
                        IconButton(onClick = { selectedIndex = null }) {
                            Icon(Icons.Default.Close, "取消选择")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                },
                actions = {
                    if (hasSelection) {
                        IconButton(onClick = {
                            val idx = selectedIndex ?: return@IconButton
                            viewModel.deleteSource(idx)
                            selectedIndex = null
                        }) {
                            Icon(Icons.Default.Delete, "删除",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, "添加源")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("免费电子书资源", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp))
                Text("点击链接在浏览器中打开各书源 · 长按可删除自定义源 · 固定源不可删除",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
            itemsIndexed(sources) { index, source ->
                val isSelected = selectedIndex == index
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (hasSelection) {
                                        selectedIndex = if (isSelected) null else index
                                    } else {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(source.url)
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                                onLongClick = {
                                    selectedIndex = if (isSelected) null else index
                                },
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.Language, null,
                            tint = if (isSelected) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(source.name, style = MaterialTheme.typography.bodyLarge)
                            Text(source.desc, fontSize = 12.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(source.url, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加书源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = { url = it },
                        label = { Text("网址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it },
                        label = { Text("描述") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        viewModel.addSource(name.trim(), url.trim(), desc.trim())
                        showAddDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}
