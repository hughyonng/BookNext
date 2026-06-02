package com.booknext.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booknext.app.data.local.db.AnnotationEntity

@Composable
fun AnnotationSidebar(
    annotations: List<AnnotationEntity>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("标注与笔记 (${annotations.size})",
                    style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "关闭")
                }
            }
            HorizontalDivider()

            if (annotations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无标注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(annotations, key = { it.id }) { ann ->
                        AnnotationCard(ann, onDelete = { onDelete(ann.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotationCard(ann: AnnotationEntity, onDelete: () -> Unit) {
    val bgColor = when (ann.type) {
        "highlight" -> Color(ann.color).copy(alpha = 0.15f)
        "underline" -> Color(0xFFE0E0E0)
        "note" -> Color(0xFFFFF3E0)
        "quote" -> Color(0xFFF3E5F5)
        else -> Color(0xFFF5F5F5)
    }
    val typeLabel = when (ann.type) {
        "highlight" -> "高亮"
        "underline" -> "划线"
        "note" -> "笔记"
        "quote" -> "摘抄"
        else -> "标注"
    }
    val typeIcon = when (ann.type) {
        "note" -> Icons.AutoMirrored.Filled.Note
        "quote" -> Icons.Default.FormatQuote
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (typeIcon != null) {
                        Icon(typeIcon, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(typeLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(14.dp))
                }
            }

            if (ann.selectedText.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    ann.selectedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (ann.note.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    ann.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}