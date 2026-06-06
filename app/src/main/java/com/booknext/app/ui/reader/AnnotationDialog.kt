package com.booknext.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.booknext.app.data.local.db.AnnotationEntity
import java.util.UUID

val HIGHLIGHT_COLORS = listOf(
    Color(0xFFFFD700) to "黄",
    Color(0xFF4CAF50) to "绿",
    Color(0xFF2196F3) to "蓝",
    Color(0xFFF44336) to "红",
)

@Composable
fun AnnotationDialog(
    bookId: String,
    selectedText: String,
    locatorJson: String,
    onDismiss: () -> Unit,
    onSave: (AnnotationEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(selectedText.take(50), maxLines = 3,
                style = MaterialTheme.typography.bodyMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择颜色：", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HIGHLIGHT_COLORS.forEach { (c, name) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.5.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        val ann = AnnotationEntity(
                                            id = UUID.randomUUID().toString(),
                                            bookId = bookId,
                                            locatorJson = locatorJson,
                                            type = "highlight",
                                            color = c.toArgb(),
                                            selectedText = selectedText,
                                        )
                                        onSave(ann)
                                        onDismiss()
                                    }
                            )
                            Text(name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("点颜色立即高亮", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
