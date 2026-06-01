package com.booknext.app.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    currentPage: DrawerPage,
    serverUrl: String,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onPageSelect: (DrawerPage) -> Unit,
    onSettingsClick: () -> Unit,
    onDarkModeToggle: () -> Unit,
    isDarkMode: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(300.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("BookNext", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(serverUrl.removePrefix("https://"), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1)
            }
        }

        Spacer(Modifier.height(8.dp))

        val navItems = listOf(
            Triple(DrawerPage.RECENT,    Icons.Default.History,     "最近阅读"),
            Triple(DrawerPage.BOOKSHELF, Icons.Default.LibraryBooks,"我的书架"),
            Triple(DrawerPage.CATEGORY,  Icons.Default.Folder,      "我的分类"),
            Triple(DrawerPage.LOCAL,     Icons.Default.PhoneAndroid, "本地文件"),
            Triple(DrawerPage.CLOUD,     Icons.Default.Cloud,        "我的云盘"),
        )

        navItems.forEach { (page, icon, label) ->
            DrawerNavItem(icon = icon, label = label,
                selected = currentPage == page, onClick = { onPageSelect(page) })
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavIconButton(Icons.Default.Settings, "设置", onClick = onSettingsClick)
            NavIconButton(
                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                if (isDarkMode) "日间" else "夜间",
                onClick = onDarkModeToggle,
            )
            if (!isLoggedIn) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onLoginClick)
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.Default.Login, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("登录", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp))
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(
                        Icons.Default.CloudDone, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("已连接", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            NavIconButton(Icons.Default.ExitToApp, "退出", onClick = onLogout,
                tint = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

@Composable
private fun DrawerNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.large).background(bgColor).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor)
    }
}

@Composable
private fun NavIconButton(
    icon: ImageVector, label: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(8.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
        Text(label, fontSize = 10.sp, color = tint, modifier = Modifier.padding(top = 2.dp))
    }
}
