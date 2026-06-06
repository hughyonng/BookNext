package com.booknext.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenStats: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            SectionTitle("服务器配置")

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onUrlChange,
                label = { Text("服务器地址") },
                placeholder = { Text("https://xxx.hf.space") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onKeyChange,
                label = { Text("访问密钥") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "隐藏" else "显示", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )

            SectionTitle("主题商店")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(com.booknext.app.ui.common.AppThemes) { theme ->
                    FilterChip(
                        selected = state.themeId == theme.id,
                        onClick = { viewModel.onThemeChange(theme.id) },
                        label = { Text(theme.name) },
                    )
                }
            }

            SectionTitle("阅读设置")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("夜间模式", style = MaterialTheme.typography.bodyLarge)
                    Text("深色背景护眼", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.darkMode,
                    onCheckedChange = viewModel::onDarkModeChange,
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("正文字号", style = MaterialTheme.typography.bodyLarge)
                    Text("${state.fontSize} sp", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = state.fontSize.toFloat(),
                    onValueChange = { viewModel.onFontSizeChange(it.toInt()) },
                    valueRange = 13f..24f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("小", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("大", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 字体家族
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("字体", style = MaterialTheme.typography.bodyLarge)
                var expanded by remember { mutableStateOf(false) }
                val fonts = listOf(
                    "serif" to "衬线（宋体）",
                    "sans-serif" to "无衬线（黑体）",
                    "monospace" to "等宽",
                    "custom" to "自定义字体…",
                )
                val fontPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
                    if (uri != null) viewModel.onCustomFontPicked(uri)
                }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(fonts.find { it.first == state.fontFamily }?.second ?: "衬线（宋体）")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        fonts.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    if (key == "custom") {
                                        expanded = false
                                        fontPicker.launch(arrayOf("font/ttf", "font/otf", "*/*"))
                                    } else {
                                        viewModel.onFontFamilyChange(key)
                                        expanded = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (state.fontFamily == "custom" && state.customFontName.isNotEmpty()) {
                Text(
                    "已选：${state.customFontName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            // 行间距
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("行间距", style = MaterialTheme.typography.bodyLarge)
                    Text("${java.lang.String.format("%.1f", state.lineSpacing)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = state.lineSpacing,
                    onValueChange = { viewModel.onLineSpacingChange(it) },
                    valueRange = 1.2f..2.8f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "这是正文预览。书中自有黄金屋，书中自有颜如玉。",
                    fontSize = TextUnit(state.fontSize.toFloat(), TextUnitType.Sp),
                    lineHeight = TextUnit(state.fontSize * 1.8f, TextUnitType.Sp),
                    modifier = Modifier.padding(16.dp),
                )
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (state.saved) "✓ 已保存" else "保存设置")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SectionTitle("账号")

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                )
            ) {
                Text("退出登录")
            }

            SectionTitle("存储")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("离线缓存", style = MaterialTheme.typography.bodyLarge)
                    Text(state.cacheSize.ifEmpty { "计算中…" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.clearCache() }) {
                    Text("清除缓存")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "BookNext v${com.booknext.app.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新输入服务器地址和密钥") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogout)
                    }
                ) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}