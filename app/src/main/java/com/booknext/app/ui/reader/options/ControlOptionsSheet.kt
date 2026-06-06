package com.booknext.app.ui.reader.options

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ControlOptions(
    val tapLeftAction: String = "prev_page",
    val tapRightAction: String = "next_page",
    val tapCenterAction: String = "toggle_ui",
    val longPressAction: String = "select_text",
    val volumeUpAction: String = "prev_page",
    val volumeDownAction: String = "next_page",
    val tapZone: String = "three_zone",
    val nineZoneConfig: String = "",
    val swipeLeftAction: String = "none",
    val swipeRightAction: String = "none",
    val swipeUpAction: String = "none",
    val swipeDownAction: String = "none",
)

val ALL_ACTIONS = listOf(
    "none" to "无",
    "prev_page" to "向上翻页",
    "next_page" to "向下翻页",
    "toggle_ui" to "显示/隐藏界面",
    "toc" to "章节目录",
    "bookmark" to "添加书签",
    "tts" to "朗读",
    "brightness_up" to "增加亮度",
    "brightness_down" to "减少亮度",
    "font_up" to "增大字体",
    "font_down" to "减小字体",
    "select_text" to "页面文字摘录",
    "search" to "搜索",
    "book_info" to "书籍信息",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlOptionsSheet(
    options: ControlOptions,
    onOptionsChange: (ControlOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var opt by remember { mutableStateOf(options) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("控制选项") },
                navigationIcon = {
                    IconButton(onClick = { onOptionsChange(opt); onDismiss() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { opt = ControlOptions() }) { Text("重置") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle("屏幕点击区域")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = opt.tapZone == "three_zone",
                    onClick = { opt = opt.copy(tapZone = "three_zone") },
                    label = { Text("三区域") },
                )
                FilterChip(
                    selected = opt.tapZone == "nine_zone",
                    onClick = { opt = opt.copy(tapZone = "nine_zone") },
                    label = { Text("九宫格") },
                )
            }

            if (opt.tapZone == "three_zone") {
                ActionDropdown("点击左边", opt.tapLeftAction) {
                    opt = opt.copy(tapLeftAction = it)
                }
                ActionDropdown("点击中间", opt.tapCenterAction) {
                    opt = opt.copy(tapCenterAction = it)
                }
                ActionDropdown("点击右边", opt.tapRightAction) {
                    opt = opt.copy(tapRightAction = it)
                }
            } else {
                NineZoneGrid(
                    config = opt.nineZoneConfig,
                    onConfigChange = { opt = opt.copy(nineZoneConfig = it) },
                )
            }

            HorizontalDivider()

            SectionTitle("长按屏幕")
            ActionDropdown("长按操作", opt.longPressAction) {
                opt = opt.copy(longPressAction = it)
            }

            HorizontalDivider()

            SectionTitle("硬件按键")
            ActionDropdown("音量增大键", opt.volumeUpAction) {
                opt = opt.copy(volumeUpAction = it)
            }
            ActionDropdown("音量减小键", opt.volumeDownAction) {
                opt = opt.copy(volumeDownAction = it)
            }

            HorizontalDivider()

            SectionTitle("屏幕划动手势")
            ActionDropdown("从左向右划动", opt.swipeRightAction) {
                opt = opt.copy(swipeRightAction = it)
            }
            ActionDropdown("从右向左划动", opt.swipeLeftAction) {
                opt = opt.copy(swipeLeftAction = it)
            }
            ActionDropdown("从上向下划动", opt.swipeDownAction) {
                opt = opt.copy(swipeDownAction = it)
            }
            ActionDropdown("从下向上划动", opt.swipeUpAction) {
                opt = opt.copy(swipeUpAction = it)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NineZoneGrid(
    config: String,
    onConfigChange: (String) -> Unit,
) {
    val defaultActions = listOf(
        "prev_page", "toggle_ui", "next_page",
        "prev_page", "toggle_ui", "next_page",
        "prev_page", "toggle_ui", "next_page",
    )
    val zones = remember(config) {
        if (config.isEmpty()) defaultActions.toMutableList()
        else config.split(",").toMutableList().apply {
            while (size < 9) add("none")
        }
    }.toMutableStateList()

    var editingZone by remember { mutableStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val actionLabel = ALL_ACTIONS.find { it.first == zones[idx] }?.second ?: "无"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { editingZone = idx },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }

    if (editingZone >= 0) {
        AlertDialog(
            onDismissRequest = { editingZone = -1 },
            title = { Text("设置区域操作") },
            text = {
                Column {
                    ALL_ACTIONS.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    zones[editingZone] = key
                                    onConfigChange(zones.joinToString(","))
                                    editingZone = -1
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = zones[editingZone] == key,
                                onClick = null,
                            )
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingZone = -1 }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ActionDropdown(label: String, current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = ALL_ACTIONS.find { it.first == current }?.second ?: "无"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(currentLabel, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ALL_ACTIONS.forEach { (key, actionLabel) ->
                    DropdownMenuItem(
                        text = { Text(actionLabel) },
                        onClick = { onSelect(key); expanded = false },
                        leadingIcon = {
                            RadioButton(selected = current == key, onClick = null)
                        }
                    )
                }
            }
        }
    }
}
