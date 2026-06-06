package com.booknext.app.ui.reader.options

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class NameReplaceItem(val from: String, val to: String)

private fun fromJson(json: String): List<NameReplaceItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            NameReplaceItem(obj.getString("from"), obj.getString("to"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun toJson(items: List<NameReplaceItem>): String {
    val arr = JSONArray()
    for (item in items) {
        val obj = JSONObject()
        obj.put("from", item.from)
        obj.put("to", item.to)
        arr.put(obj)
    }
    return arr.toString()
}

@Composable
fun NameReplaceSheet(
    json: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var items by remember(json) { mutableStateOf(fromJson(json)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("名字替换", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = item.from,
                            onValueChange = { v ->
                                items = items.toMutableList().also { it[index] = item.copy(from = v) }
                            },
                            label = { Text("原词") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = item.to,
                            onValueChange = { v ->
                                items = items.toMutableList().also { it[index] = item.copy(to = v) }
                            },
                            label = { Text("替换为") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        IconButton(onClick = {
                            items = items.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            TextButton(onClick = {
                items = items + NameReplaceItem("", "")
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加规则")
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(onClick = { onSave(toJson(items)) }) { Text("保存") }
            }
        }
    }
}
