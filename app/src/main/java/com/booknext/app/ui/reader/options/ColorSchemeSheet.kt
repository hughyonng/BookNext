package com.booknext.app.ui.reader.options

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ColorPreset(
    val name: String,
    val bgColor: Color,
    val textColor: Color,
)

private val presets = listOf(
    ColorPreset("默认", Color(0xFFF9F7F4), Color(0xFF1A1A1A)),
    ColorPreset("护眼", Color(0xFFC7EDCC), Color(0xFF1A1A1A)),
    ColorPreset("夜间", Color(0xFF1A1814), Color(0xFFE0D8CC)),
    ColorPreset("羊皮纸", Color(0xFFF5E6C8), Color(0xFF3E2723)),
    ColorPreset("淡蓝", Color(0xFFD6EAF8), Color(0xFF1A237E)),
    ColorPreset("灰色", Color(0xFFE0E0E0), Color(0xFF212121)),
    ColorPreset("墨绿", Color(0xFF1B3B1B), Color(0xFFA8D5A8)),
    ColorPreset("怀旧", Color(0xFFE8D5B7), Color(0xFF5D4037)),
)

@Composable
fun ColorSchemeSheet(
    onApply: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var customBgColor by remember { mutableStateOf(Color(0xFFF9F7F4)) }
    var customTextColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }
    var useCustom by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("配色方案", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            Text("预设方案", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(presets.size) { index ->
                    val preset = presets[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedPresetIndex = index
                                useCustom = false
                            }
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(preset.bgColor)
                                .border(
                                    width = if (!useCustom && selectedPresetIndex == index) 3.dp else 1.dp,
                                    color = if (!useCustom && selectedPresetIndex == index) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(preset.textColor),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(preset.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("自定义", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("背景", style = MaterialTheme.typography.labelSmall)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(customBgColor)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                useCustom = true
                            },
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("文字", style = MaterialTheme.typography.labelSmall)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(customTextColor)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                useCustom = true
                            },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        val (bg, text) = if (useCustom) {
                            customBgColor to customTextColor
                        } else {
                            val p = presets[selectedPresetIndex]
                            p.bgColor to p.textColor
                        }
                        onApply(text.toArgb().toLong(), bg.toArgb().toLong())
                    },
                ) { Text("应用") }
            }
        }
    }
}
