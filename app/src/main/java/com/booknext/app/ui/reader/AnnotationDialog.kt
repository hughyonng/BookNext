package com.booknext.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    var selectedColor by remember { mutableStateOf(HIGHLIGHT_COLORS[0].first) }
    var noteText by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(selectedText.ifEmpty { "标注" }, maxLines = 3,
                style = MaterialTheme.typography.bodyMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 标注类型按钮行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AnnotationTypeButton("高亮", color = selectedColor, onClick = {
                        val ann = AnnotationEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = bookId,
                            locatorJson = locatorJson,
                            type = "highlight",
                            color = selectedColor.toArgb(),
                            selectedText = selectedText,
                            note = noteText,
                        )
                        onSave(ann)
                        onDismiss()
                    })
                    AnnotationTypeButton("划线", color = Color.Gray, onClick = {
                        val ann = AnnotationEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = bookId,
                            locatorJson = locatorJson,
                            type = "underline",
                            color = Color.Gray.toArgb(),
                            selectedText = selectedText,
                        )
                        onSave(ann)
                        onDismiss()
                    })
                    AnnotationTypeButton("笔记", color = Color(0xFFFF9800), onClick = {
                        showNoteField = true
                    })
                    AnnotationTypeButton("摘抄", color = Color(0xFF9C27B0), onClick = {
                        val ann = AnnotationEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = bookId,
                            locatorJson = locatorJson,
                            type = "quote",
                            color = Color(0xFF9C27B0).toArgb(),
                            selectedText = selectedText,
                        )
                        onSave(ann)
                        onDismiss()
                    })
                }

                // 颜色选择
                TextButton(onClick = { showColorPicker = !showColorPicker }) {
                    Text("颜色 ▾", style = MaterialTheme.typography.labelSmall)
                }
                if (showColorPicker) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HIGHLIGHT_COLORS.forEach { (c, name) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (c == selectedColor) 3.dp else 1.dp,
                                            color = if (c == selectedColor) Color.Black else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = c }
                                )
                                Text(name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 笔记输入
                if (showNoteField) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("笔记内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    TextButton(onClick = {
                        val ann = AnnotationEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = bookId,
                            locatorJson = locatorJson,
                            type = "note",
                            color = Color(0xFFFF9800).toArgb(),
                            selectedText = selectedText,
                            note = noteText,
                        )
                        onSave(ann)
                        onDismiss()
                    }) {
                        Text("保存笔记")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun AnnotationTypeButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}