package com.booknext.app.ui.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.booknext.app.data.local.db.BookEntity
import com.booknext.app.data.local.db.ReadingSessionEntity
import com.booknext.app.ui.reader.options.VisualOptions
import com.booknext.app.ui.reader.options.VisualOptionsSheet
import kotlinx.coroutines.launch

data class TocEntry(val title: String, val index: Int)

data class ReaderToolbarState(
    val title: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val darkMode: Boolean = false,
    val fontSize: Int = 17,
    val brightness: Float = -1f,
    val screenOrientation: String = "auto",
    val isTtsPlaying: Boolean = false,
    val tocEntries: List<TocEntry> = emptyList(),
    val bookmarks: List<Int> = emptyList(),
)

@Composable
fun ReaderToolbarOverlay(
    state: ReaderToolbarState,
    visible: Boolean,
    onBack: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onOrientationChange: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onTtsStart: () -> Unit,
    onTtsStop: () -> Unit,
    onTocJump: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onAutoScroll: (mode: String, speed: Float) -> Unit = { _, _ -> },
    onSearch: (query: String, nthMatch: Int) -> Unit = { _, _ -> },
    onReplaceAll: (from: String, to: String) -> Unit = { _, _ -> },
    book: BookEntity? = null,
    sessions: List<ReadingSessionEntity> = emptyList(),
    coverUrl: String? = null,
    onToggleFavorite: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onPageTextCopy: () -> Unit = {},
    onBookmarkManage: () -> Unit = {},
    currentVisualOptions: VisualOptions = VisualOptions(),
    currentControlOptions: com.booknext.app.ui.reader.options.ControlOptions = com.booknext.app.ui.reader.options.ControlOptions(),
    currentOtherOptions: com.booknext.app.ui.reader.options.OtherOptions = com.booknext.app.ui.reader.options.OtherOptions(),
    onSaveVisualOptions: (VisualOptions) -> Unit = {},
    onSaveControlOptions: (com.booknext.app.ui.reader.options.ControlOptions) -> Unit = {},
    onSaveOtherOptions: (com.booknext.app.ui.reader.options.OtherOptions) -> Unit = {},
    nameReplacements: String = "",
    onNameReplaceChange: (String) -> Unit = {},
    onSaveSetting: (key: String, value: Any) -> Unit = { _, _ -> },
    onSetTranslateEngine: (String) -> Unit = {},
    onSetTranslateTargetLang: (String) -> Unit = {},
    onTranslateText: () -> Unit = {},
    onDictionaryLookup: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showToc by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showBrightnessPanel by remember { mutableStateOf(false) }
    var showFontPanel by remember { mutableStateOf(false) }
    var showTtsPanel by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showVisualOptions by remember { mutableStateOf(false) }
    var showControlOptions by remember { mutableStateOf(false) }
    var showOtherOptions by remember { mutableStateOf(false) }
    var showNameReplace by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showTranslateSettings by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(
                        text = "${state.currentPage + 1} / ${state.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Box {
                        IconButton(onClick = { showMoreActions = !showMoreActions }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showMoreActions,
                            onDismissRequest = { showMoreActions = false },
                        ) {
                            listOf(
                                "字体设置" to "Font",
                                "目录" to "Toc",
                                "书签" to "Bookmarks",
                                "搜索" to "Search",
                                "词典" to "Dictionary",
                                "翻译设置" to "TranslateSettings",
                            ).forEach { (label, action) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showMoreActions = false
                                        when (action) {
                                            "Font" -> showVisualOptions = true
                                            "Toc" -> showToc = true
                                            "Bookmarks" -> showBookmarks = true
                                            "Search" -> showSearchBar = true
                                            "Dictionary" -> onDictionaryLookup()
                                            "TranslateSettings" -> showTranslateSettings = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${state.currentPage + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = state.currentPage.toFloat(),
                            onValueChange = { onPageChange(it.toInt()) },
                            valueRange = 0f..(state.totalPages - 1).toFloat().coerceAtLeast(1f),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text(
                            "${state.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ToolbarIconButton(
                            icon = if (state.darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            label = if (state.darkMode) "日间" else "夜间",
                            onClick = { onDarkModeChange(!state.darkMode) },
                            onLongClick = {},
                        )
                        ToolbarIconButton(
                            icon = Icons.Default.Brightness6,
                            label = "亮度",
                            onClick = {
                                showBrightnessPanel = !showBrightnessPanel
                                showFontPanel = false
                                showTtsPanel = false
                            },
                            onLongClick = {},
                        )
                        ToolbarIconButton(
                            icon = Icons.Default.Translate,
                            label = "翻译",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onTranslateText() },
                            onLongClick = {},
                        )
                        ToolbarIconButton(
                            icon = Icons.Default.FormatSize,
                            label = "字号",
                            onClick = {
                                showFontPanel = !showFontPanel
                                showBrightnessPanel = false
                                showTtsPanel = false
                            },
                            onLongClick = {},
                        )
                        ToolbarIconButton(
                            icon = Icons.Default.RecordVoiceOver,
                            label = "朗读",
                            tint = if (state.isTtsPlaying) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current,
                            onClick = {
                                if (state.isTtsPlaying) {
                                    showTtsPanel = !showTtsPanel
                                } else {
                                    onTtsStart()
                                    showTtsPanel = true
                                    showFontPanel = false
                                    showBrightnessPanel = false
                                }
                            },
                            onLongClick = {},
                        )
                    }

                    AnimatedVisibility(visible = showBrightnessPanel) {
                        BrightnessPanel(
                            brightness = state.brightness,
                            onBrightnessChange = { v ->
                                onBrightnessChange(v)
                                applyBrightness(activity, v)
                            },
                        )
                    }

                    AnimatedVisibility(visible = showFontPanel) {
                        FontPanel(
                            fontSize = state.fontSize,
                            onFontSizeChange = onFontSizeChange,
                        )
                    }

                    AnimatedVisibility(visible = showTtsPanel) {
                        TtsControlPanel(
                            isTtsPlaying = state.isTtsPlaying,
                            onStop = { onTtsStop(); showTtsPanel = false },
                            onTogglePlay = { if (state.isTtsPlaying) onTtsStop() else onTtsStart() },
                        )
                    }
                }
            }
        }

        if (showToc) {
            TocPanel(
                entries = state.tocEntries,
                currentPage = state.currentPage,
                onJump = { idx -> onTocJump(idx); showToc = false },
                onDismiss = { showToc = false },
            )
        }

        if (showBookmarks) {
            BookmarkPanel(
                bookmarks = state.bookmarks,
                onJump = { pg -> onPageChange(pg); showBookmarks = false },
                onAdd = { onAddBookmark(); showBookmarks = false },
                onDismiss = { showBookmarks = false },
            )
        }

        if (showVisualOptions) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                VisualOptionsSheet(
                    options = currentVisualOptions,
                    onOptionsChange = { onSaveVisualOptions(it); showVisualOptions = false },
                    onDismiss = { showVisualOptions = false },
                )
            }
        }

        if (showTranslateSettings) {
            Dialog(
                onDismissRequest = { showTranslateSettings = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                TranslateSettingsPanel(
                    currentEngine = "google",
                    currentTargetLang = "zh-CN",
                    onSetEngine = onSetTranslateEngine,
                    onSetTargetLang = onSetTranslateTargetLang,
                    onDismiss = { showTranslateSettings = false },
                )
            }
        }

        if (showSearchBar) {
            FindReplaceDialog(
                onSearch = { query, nth -> onSearch(query, nth) },
                onReplaceAll = { from, to -> onReplaceAll(from, to) },
                onDismiss = { showSearchBar = false },
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    tint: Color = LocalContentColor.current,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BrightnessPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
) {
    var followSystem by remember { mutableStateOf(brightness < 0f) }
    var eyeProtect by remember { mutableStateOf(false) }
    var localBrightness by remember(brightness) {
        mutableStateOf(if (brightness < 0f) 0.5f else brightness.coerceIn(0.01f, 1f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BrightnessLow, null, modifier = Modifier.size(18.dp))
            Slider(
                value = if (followSystem) 0.5f else localBrightness,
                onValueChange = { if (!followSystem) { localBrightness = it; onBrightnessChange(it) } },
                valueRange = 0.01f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                enabled = !followSystem,
            )
            Icon(Icons.Default.BrightnessHigh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("随系统", style = MaterialTheme.typography.labelSmall)
            Switch(
                checked = followSystem,
                onCheckedChange = {
                    followSystem = it
                    if (it) onBrightnessChange(-1f)
                },
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("护眼", style = MaterialTheme.typography.labelSmall)
            Switch(
                checked = eyeProtect,
                onCheckedChange = { eyeProtect = it },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun FontPanel(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledTonalIconButton(
            onClick = { onFontSizeChange((fontSize - 1).coerceAtLeast(11)) },
            modifier = Modifier.size(40.dp),
        ) {
            Text("-", fontSize = 20.sp)
        }
        Text(
            text = "${fontSize}sp",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(min = 64.dp),
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onFontSizeChange((fontSize + 1).coerceAtMost(28)) },
            modifier = Modifier.size(40.dp),
        ) {
            Text("+", fontSize = 20.sp)
        }
    }
}

@Composable
private fun TtsControlPanel(
    isTtsPlaying: Boolean,
    onStop: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    var volume by remember { mutableFloatStateOf(50f) }
    var pitch by remember { mutableFloatStateOf(10f) }
    var speed by remember { mutableFloatStateOf(10f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TtsSliderRow("音量", volume, 0f, 100f) { volume = it }
        TtsSliderRow("音调", pitch, 0f, 20f) { pitch = it }
        TtsSliderRow("语速", speed, 0f, 20f) { speed = it }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onStop) { Icon(Icons.Default.Stop, "停止") }
            IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, "上一句") }
            IconButton(onClick = {}) { Icon(Icons.Default.FastRewind, "快退") }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isTtsPlaying) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = {}) { Icon(Icons.Default.FastForward, "快进") }
            IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, "下一句") }
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, "设置") }
        }
    }
}

@Composable
private fun TtsSliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(36.dp))
        Text(value.toInt().toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(28.dp))
        Slider(
            value = value, onValueChange = onValueChange,
            valueRange = min..max, modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(min)) },
            modifier = Modifier.size(32.dp),
        ) { Text("-") }
        IconButton(
            onClick = { onValueChange((value + 1).coerceAtMost(max)) },
            modifier = Modifier.size(32.dp),
        ) { Text("+") }
    }
}

@Composable
private fun TocPanel(
    entries: List<TocEntry>,
    currentPage: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("目录", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                if (entries.isEmpty()) {
                    Text("暂无目录", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(entries.size) { i ->
                            val entry = entries[i]
                            val isCurrent = entry.index == currentPage
                            TextButton(
                                onClick = { onJump(entry.index) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    entry.title,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                    style = if (isCurrent) MaterialTheme.typography.bodyMedium
                                            else MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun BookmarkPanel(
    bookmarks: List<Int>,
    onJump: (Int) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("书签", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                if (bookmarks.isEmpty()) {
                    Text("暂无书签", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(bookmarks.size) { i ->
                            TextButton(onClick = { onJump(bookmarks[i]) }, modifier = Modifier.fillMaxWidth()) {
                                Text("第 ${bookmarks[i] + 1} 页")
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onAdd) { Text("添加书签") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun FindReplaceDialog(
    onSearch: (String, Int) -> Unit,
    onReplaceAll: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var nthMatch by remember { mutableIntStateOf(0) }
    var searched by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp).statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Text("搜索 / 替换", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = findText,
                onValueChange = { findText = it; searched = false },
                label = { Text("搜索内容") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (findText.isNotBlank()) {
                        IconButton(onClick = { findText = ""; searched = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = replaceText,
                onValueChange = { replaceText = it },
                label = { Text("替换为") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { nthMatch = 0; searched = true; onSearch(findText, 0); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                enabled = findText.isNotBlank(),
            ) { Text("搜索") }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { nthMatch++; onSearch(findText, nthMatch) },
                    modifier = Modifier.weight(1f),
                    enabled = findText.isNotBlank() && searched,
                ) { Text("下一个") }

                OutlinedButton(
                    onClick = { onReplaceAll(findText, replaceText); searched = false },
                    modifier = Modifier.weight(1f),
                    enabled = findText.isNotBlank(),
                ) { Text("替换全部") }
            }
        }
    }
}

@Composable
private fun TranslateSettingsPanel(
    currentEngine: String,
    currentTargetLang: String,
    onSetEngine: (String) -> Unit,
    onSetTargetLang: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedEngine by remember { mutableStateOf(currentEngine) }
    var selectedLang by remember { mutableStateOf(currentTargetLang) }
    val engines = listOf(
        "google" to "谷歌翻译（免费）",
        "baidu" to "百度翻译",
        "youdao" to "有道翻译",
    )
    val languages = listOf(
        "zh-CN" to "中文（简体）",
        "zh-TW" to "中文（繁体）",
        "en" to "英文",
        "ja" to "日文",
        "ko" to "韩文",
        "fr" to "法文",
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("翻译设置", style = MaterialTheme.typography.titleMedium)
            Text("在线翻译", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            engines.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedEngine == key,
                        onClick = { selectedEngine = key },
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text("目标语言", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            languages.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedLang == key,
                        onClick = { selectedLang = key },
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onSetEngine(selectedEngine)
                    onSetTargetLang(selectedLang)
                    onDismiss()
                }) { Text("确定") }
            }
        }
    }
}

fun applyOrientation(activity: Activity?, key: String) {
    activity?.requestedOrientation = when (key) {
        "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        "portrait_reverse" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        "landscape_reverse" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

fun applyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    val attrs = window.attributes
    attrs.screenBrightness = if (value < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else value
    window.attributes = attrs
}
