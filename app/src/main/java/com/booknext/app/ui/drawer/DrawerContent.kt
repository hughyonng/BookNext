package com.booknext.app.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .navigationBarsPadding(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) Color(0xFF2A2A2A) else Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Image(
                            painter = painterResource(
                                if (isDarkMode) com.booknext.app.R.drawable.drawer_logo_night
                                else com.booknext.app.R.drawable.drawer_logo
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.15f),
                        )
                    }
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
            Triple(DrawerPage.RECENT,    Icons.Default.History,      "最近阅读"),
            Triple(DrawerPage.BOOKSHELF, Icons.AutoMirrored.Filled.LibraryBooks, "我的书架"),
            Triple(DrawerPage.CATEGORY,  Icons.Default.Folder,       "我的分类"),
            Triple(DrawerPage.LOCAL,     Icons.Default.PhoneAndroid, "本地文件"),
            Triple(DrawerPage.CLOUD,     Icons.Default.Cloud,        "我的云盘"),
            Triple(DrawerPage.ONLINE_LIBRARY, Icons.Default.Language,"网上书库"),
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
            if (isLoggedIn) {
                NavIconButton(Icons.Default.CheckCircle, "已登录", onClick = {},
                    tint = Color(0xFF4CAF50))
            } else {
                NavIconButton(Icons.Default.AccountCircle, "未登录", onClick = {})
            }
            NavIconButton(Icons.AutoMirrored.Filled.ExitToApp, "退出", onClick = onLogout,
                tint = MaterialTheme.colorScheme.error)
        }

        if (!isLoggedIn) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("登录")
            }
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
    icon: ImageVector, label: String, onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(8.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
        Text(label, fontSize = 10.sp, color = tint, modifier = Modifier.padding(top = 2.dp))
    }
}
