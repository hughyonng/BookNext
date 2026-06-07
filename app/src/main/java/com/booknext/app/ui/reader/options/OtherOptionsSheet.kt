package com.booknext.app.ui.reader.options

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class OtherOptions(
    val showStatusBar: Boolean = false,
    val showNavBar: Boolean = false,
    val showProgressBar: Boolean = false,
    val keepScreenOn: Boolean = true,
    val edgeSwipeBrightness: Boolean = true,
    val edgeSwipeFontSize: Boolean = false,
    val keepLastLine: Boolean = false,
    val smartIndent: Boolean = true,
    val removeExtraBlank: Boolean = false,
    val blueLight: Boolean = false,
    val blueLightAmount: Float = 0.3f,
    val readingReminder: Boolean = false,
    val readingReminderMins: Int = 30,
    val ttsSplitMode: String = "sentence",
    val allowTiltFlip: Boolean = false,
    val allowPinchFont: Boolean = true,
    val allowSwipeFlip: Boolean = true,
    val epubUseBookFont: Boolean = true,
    val epubDisableCss: Boolean = false,
    val epubShowAnnotations: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherOptionsSheet(
    options: OtherOptions,
    onOptionsChange: (OtherOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var opt by remember { mutableStateOf(options) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("其它选项") },
                navigationIcon = {
                    IconButton(onClick = { onOptionsChange(opt); onDismiss() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { opt = OtherOptions() }) { Text("重置") }
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle("屏幕与显示")
            ToggleRow("阅读时保持屏幕长亮", opt.keepScreenOn) {
                opt = opt.copy(keepScreenOn = it)
            }
            ToggleRow("全屏时显示透明系统通知栏", opt.showStatusBar) {
                opt = opt.copy(showStatusBar = it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("边缘手势")
            ToggleRow("擦动屏幕左边沿调节亮度", opt.edgeSwipeBrightness) {
                opt = opt.copy(edgeSwipeBrightness = it)
            }
            ToggleRow("擦动屏幕右边沿调节字体大小", opt.edgeSwipeFontSize) {
                opt = opt.copy(edgeSwipeFontSize = it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("翻页行为")
            ToggleRow("翻页时保留一行", opt.keepLastLine) {
                opt = opt.copy(keepLastLine = it)
            }
            ToggleRow("允许倾斜设备翻页", opt.allowTiltFlip) {
                opt = opt.copy(allowTiltFlip = it)
            }
            ToggleRow("允许左右划动屏幕翻页", opt.allowSwipeFlip) {
                opt = opt.copy(allowSwipeFlip = it)
            }
            ToggleRow("允许用多点触摸改变字体大小", opt.allowPinchFont) {
                opt = opt.copy(allowPinchFont = it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("智能排版（TXT）")
            ToggleRow("段落首行缩进", opt.smartIndent) {
                opt = opt.copy(smartIndent = it)
            }
            ToggleRow("删除多余空格和空行", opt.removeExtraBlank) {
                opt = opt.copy(removeExtraBlank = it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("EPUB 专项")
            ToggleRow("使用书籍内置字体", opt.epubUseBookFont) {
                opt = opt.copy(epubUseBookFont = it)
            }
            ToggleRow("禁用书籍内置 CSS 样式表", opt.epubDisableCss) {
                opt = opt.copy(epubDisableCss = it)
            }
            ToggleRow("在章节里直接显示注解内容", opt.epubShowAnnotations) {
                opt = opt.copy(epubShowAnnotations = it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("护视力")
            ToggleRow("蓝色光波过滤", opt.blueLight) {
                opt = opt.copy(blueLight = it)
            }
            AnimatedVisibility(visible = opt.blueLight) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Text("过滤量", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = opt.blueLightAmount,
                        onValueChange = { opt = opt.copy(blueLightAmount = it) },
                        valueRange = 0.05f..0.8f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${(opt.blueLightAmount * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            ToggleRow("连续阅读时间提醒", opt.readingReminder) {
                opt = opt.copy(readingReminder = it)
            }
            AnimatedVisibility(visible = opt.readingReminder) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Text("提醒间隔", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = opt.readingReminderMins.toFloat(),
                        onValueChange = { opt = opt.copy(readingReminderMins = it.toInt()) },
                        valueRange = 10f..120f,
                        steps = 10,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${opt.readingReminderMins} 分钟",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("TTS 朗读设置")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("内容划分方式", style = MaterialTheme.typography.bodyMedium)
                var expanded by remember { mutableStateOf(false) }
                val modes = listOf("sentence" to "按句号", "paragraph" to "按段落", "page" to "按页面")
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(modes.find { it.first == opt.ttsSplitMode }?.second ?: "按句号")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        modes.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { opt = opt.copy(ttsSplitMode = key); expanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
