package com.booknext.app.ui.reader.options

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class VisualOptions(
    val fontSize: Int = 17,
    val lineSpacing: Float = 1.8f,
    val fontFamily: String = "serif",
    val customFontPath: String = "",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VisualOptionsSheet(
    options: VisualOptions,
    onOptionsChange: (VisualOptions) -> Unit,
    onDismiss: () -> Unit,
    onUploadCustomFont: (Uri) -> Unit = {},
) {
    var opt by remember { mutableStateOf(options) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val fileName = "custom_${System.currentTimeMillis()}.ttf"
                val targetDir = File(ctx.filesDir, "fonts")
                targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val fontPath = targetFile.absolutePath
                opt = opt.copy(fontFamily = "custom", customFontPath = fontPath)
                onOptionsChange(opt.copy(fontFamily = "custom", customFontPath = fontPath))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字体设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        onOptionsChange(opt)
                        onDismiss()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { opt = VisualOptions() }) { Text("重置") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle("文字大小")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("A", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = opt.fontSize.toFloat(),
                    onValueChange = { opt = opt.copy(fontSize = it.toInt()) },
                    valueRange = 11f..28f,
                    modifier = Modifier.weight(1f),
                )
                Text("A", fontSize = 22.sp)
                Text("${opt.fontSize}sp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp))
            }

            SectionTitle("字体")
            val fonts = listOf(
                "serif" to "衬线（宋体）",
                "sans-serif" to "无衬线（黑体）",
                "sans-serif-light" to "细体",
                "sans-serif-medium" to "中黑体",
                "monospace" to "等宽",
                "cursive" to "手写体",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fonts.forEach { (key, label) ->
                    FilterChip(
                        selected = opt.fontFamily == key,
                        onClick = { opt = opt.copy(fontFamily = key) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { fontPicker.launch("*/*") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (opt.fontFamily == "custom") "已上传字体" else "上传自定义字体",
                        style = MaterialTheme.typography.labelSmall)
                }
                if (opt.fontFamily == "custom") {
                    TextButton(onClick = {
                        opt = opt.copy(fontFamily = "serif", customFontPath = "")
                        onOptionsChange(opt.copy(fontFamily = "serif", customFontPath = ""))
                    }) { Text("恢复默认", style = MaterialTheme.typography.labelSmall) }
                }
            }

            SectionTitle("行间距")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("行间距",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = opt.lineSpacing,
                    onValueChange = { opt = opt.copy(lineSpacing = it) },
                    valueRange = 1.0f..3.0f,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    String.format("%.1f", opt.lineSpacing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(36.dp),
                )
            }

            SectionTitle("预览")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "书中自有黄金屋，书中自有颜如玉。阅读是人类进步的阶梯，知识改变命运。",
                    fontSize = opt.fontSize.sp,
                    lineHeight = (opt.fontSize * opt.lineSpacing).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
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
        modifier = Modifier.padding(top = 4.dp),
    )
}
