package com.booknext.app.ui.quotes

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.AnnotationDao
import com.booknext.app.data.local.db.AnnotationEntity
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.BookEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuotesState(
    val annotations: List<Pair<BookEntity?, List<AnnotationEntity>>> = emptyList(),
    val totalAnnotations: Int = 0,
    val totalBooks: Int = 0,
)

@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val annotationDao: AnnotationDao,
) : ViewModel() {

    private val _state = MutableStateFlow(QuotesState())
    val state: StateFlow<QuotesState> = _state

    init {
        viewModelScope.launch {
            combine(
                bookDao.observeAll(),
                annotationDao.observeAll()
            ) { books, annotations ->
                val grouped = annotations.groupBy { it.bookId }
                val pairs = books.filter { it.bookId in grouped }
                    .map { book -> book to (grouped[book.bookId] ?: emptyList()) }
                QuotesState(
                    annotations = pairs,
                    totalAnnotations = annotations.size,
                    totalBooks = grouped.size,
                )
            }.collect { _state.value = it }
        }
    }

    fun buildMarkdown(): String {
        val sb = StringBuilder()
        sb.appendLine("# BookNext 摘抄本")
        sb.appendLine()
        _state.value.annotations.forEach { (book, anns) ->
            sb.appendLine("## ${book?.title ?: "未知书籍"}")
            sb.appendLine()
            anns.forEach { ann ->
                val prefix = when (ann.type) {
                    "highlight" -> "🟨"
                    "underline" -> "➖"
                    "note" -> "📝"
                    "quote" -> "💬"
                    else -> "•"
                }
                sb.appendLine("$prefix ${ann.selectedText}")
                if (ann.note.isNotEmpty()) {
                    sb.appendLine("  > ${ann.note}")
                }
                sb.appendLine()
            }
        }
        return sb.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesScreen(
    onBack: () -> Unit,
    viewModel: QuotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("摘抄本") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (state.totalAnnotations > 0) {
                        IconButton(onClick = {
                            val md = viewModel.buildMarkdown()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, md)
                                putExtra(Intent.EXTRA_SUBJECT, "BookNext 摘抄本")
                            }
                            context.startActivity(Intent.createChooser(intent, "导出摘抄"))
                        }) {
                            Icon(Icons.Default.Share, "导出")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.totalAnnotations == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无标注", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("长按阅读中的文字可以添加标注",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${state.totalBooks} 本书，${state.totalAnnotations} 条标注",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    state.annotations.forEach { (book, anns) ->
                        item {
                            Text(
                                "${book?.title ?: "未知"} (${anns.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(anns, key = { it.id }) { ann ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val typeLabel = when (ann.type) {
                                        "highlight" -> "高亮"
                                        "underline" -> "划线"
                                        "note" -> "笔记"
                                        "quote" -> "摘抄"
                                        else -> "标注"
                                    }
                                    Text(typeLabel, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(ann.selectedText, style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    if (ann.note.isNotEmpty()) {
                                        Text("  ↳ ${ann.note}",
                                            style = MaterialTheme.typography.bodySmall,
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
}