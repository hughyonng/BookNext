package com.booknext.app.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TextActionMenu(
    text: String,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onAnnotate: () -> Unit,
    onDictionary: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = text.take(100) + if (text.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ActionButton("复制", onClick = onCopy)
                    ActionButton("高亮", onClick = onHighlight)
                    ActionButton("标注", onClick = onAnnotate)
                    ActionButton("词典", onClick = onDictionary)
                    ActionButton("更多", onClick = onMore)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
