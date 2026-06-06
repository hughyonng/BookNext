package com.booknext.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class MoreAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun MoreActionsSheet(
    onDismiss: () -> Unit,
    onTtsStart: () -> Unit,
    onPageTextCopy: () -> Unit,
    onSearch: () -> Unit,
    onAutoScroll: () -> Unit,
    onToc: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onBookmarkManage: () -> Unit,
    onFontSize: () -> Unit,
    onBrightness: () -> Unit,
) {
    val actions = listOf(
        MoreAction(Icons.Default.RecordVoiceOver, "朗读") {
            onTtsStart(); onDismiss()
        },
        MoreAction(Icons.Default.ContentCopy, "页面文字摘录") {
            onPageTextCopy(); onDismiss()
        },
        MoreAction(Icons.Default.Search, "全文搜索") {
            onSearch(); onDismiss()
        },
        MoreAction(Icons.Default.SwapVert, "自动滚屏") {
            onAutoScroll(); onDismiss()
        },
        MoreAction(Icons.Default.FormatListBulleted, "章节目录") {
            onToc(); onDismiss()
        },
        MoreAction(Icons.Default.SkipPrevious, "切换到上一章节") {
            onPrevChapter(); onDismiss()
        },
        MoreAction(Icons.Default.SkipNext, "切换到下一章节") {
            onNextChapter(); onDismiss()
        },
        MoreAction(Icons.Default.Bookmark, "书签管理") {
            onBookmarkManage(); onDismiss()
        },
        MoreAction(Icons.Default.FormatSize, "字体大小调节") {
            onFontSize(); onDismiss()
        },
        MoreAction(Icons.Default.Brightness6, "亮度调节") {
            onBrightness(); onDismiss()
        },
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "更多操作",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()
            LazyColumn {
                items(actions) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action.onClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            action.icon,
                            contentDescription = action.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(action.label, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
