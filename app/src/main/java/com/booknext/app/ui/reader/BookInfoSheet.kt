package com.booknext.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoSheet(
    book: BookEntity,
    sessions: List<ReadingSessionEntity>,
    totalPages: Int,
    currentPage: Int,
    coverUrl: String?,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val scrollState = rememberScrollState()

    val readingHours = book.totalReadingSeconds / 3600f
    val avgWpm = if (sessions.isNotEmpty())
        sessions.filter { it.wordsPerMinute > 0 }.map { it.wordsPerMinute }.average()
            .takeIf { !it.isNaN() }?.roundToInt() ?: 0
    else 0

    val totalCharsEstimate = when (book.format) {
        "pdf" -> (book.pageCount ?: totalPages) * 500L
        else -> 100_000L
    }
    val remainingChars = ((1f - book.readingPercent) * totalCharsEstimate).toLong()
        .coerceAtLeast(0L)
    val chapterRemainingMins = if (avgWpm > 0) (remainingChars / avgWpm / 20).toInt() else 0
    val bookRemainingMins = if (avgWpm > 0) (remainingChars / avgWpm).toInt() else 0

    val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍信息") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    Row {
                        repeat(5) { i ->
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Star
                                              else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (book.isFavorite) Color(0xFFFFC107)
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = {}) { Text("更多…") }
                TextButton(onClick = onToggleFavorite) {
                    Text(if (book.isFavorite) "取消珍藏" else "加入珍藏")
                }
                TextButton(onClick = onDismiss) { Text("确定") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(220.dp)
                            .width(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(book.title.take(4), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (book.author.isNotEmpty()) {
                Text(
                    text = "- ${book.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }

            HorizontalDivider()

            InfoSection(title = "文件信息") {
                InfoRow("文件名", book.bookId + ".${book.format}")
                InfoRow("格式", book.format.uppercase())
                InfoRow("文件大小", formatFileSize(book.fileSize))
                InfoRow("总页数", if (totalPages > 0) "$totalPages 页" else "—")
                InfoRow("当前页码", "${currentPage + 1}")
                InfoRow("阅读进度", "${(book.readingPercent * 100).roundToInt()}%")
            }

            HorizontalDivider()

            InfoSection(title = "阅读统计") {
                InfoRow("阅读时数",
                    if (readingHours >= 1f) String.format("%.2f 小时", readingHours)
                    else "${book.totalReadingSeconds / 60} 分钟")
                InfoRow("阅读速度",
                    if (avgWpm > 0) "$avgWpm 字/分钟" else "—")
                if (book.lastReadAt > 0) {
                    InfoRow("最后阅读", dateFormat.format(Date(book.lastReadAt)))
                }
                if (chapterRemainingMins > 0) {
                    InfoRow("章节剩余时间", "${chapterRemainingMins} 分钟")
                }
                if (bookRemainingMins > 0) {
                    val h = bookRemainingMins / 60
                    val m = bookRemainingMins % 60
                    InfoRow("整书剩余时间",
                        if (h > 0) "${h} 小时 ${m} 分钟" else "${m} 分钟")
                }
            }

            if (sessions.isNotEmpty()) {
                HorizontalDivider()
                InfoSection(title = "阅读日期") {
                    sessions.take(5).forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                dateFormat.format(Date(session.startTime)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                buildString {
                                    val mins = session.durationSeconds / 60
                                    val secs = session.durationSeconds % 60
                                    append("${mins}'${secs.toString().padStart(2,'0')}\"")
                                    if (session.wordsPerMinute > 0)
                                        append(" ${session.wordsPerMinute}字/分钟")
                                    append(" ${(session.progressPercent * 100).roundToInt()}%")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes <= 0L -> "未知"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / 1024f / 1024f)
    }
}
