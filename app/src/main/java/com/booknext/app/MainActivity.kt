package com.booknext.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.ui.bookshelf.BookshelfScreen
import com.booknext.app.ui.category.CategoryScreen
import com.booknext.app.ui.cloud.CloudScreen
import com.booknext.app.ui.common.BookNextTheme
import com.booknext.app.ui.drawer.DrawerContent
import com.booknext.app.ui.drawer.DrawerPage
import com.booknext.app.ui.local.LocalScreen
import com.booknext.app.ui.onlinelibrary.OnlineLibraryScreen
import com.booknext.app.ui.recent.RecentScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppState { WELCOME, LOGIN, MAIN }

val LocalActivity = staticCompositionLocalOf<FragmentActivity> {
    error("No Activity provided")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeId by prefs.themeId.collectAsState(initial = "blue")
            val darkMode by prefs.darkMode.collectAsState(initial = false)
            val uiFontScale by prefs.uiFontScale.collectAsState(initial = 1.0f)
            val uiFontFamily by prefs.uiFontFamily.collectAsState(initial = "sans-serif")
            val uiLineSpacing by prefs.uiLineSpacing.collectAsState(initial = 1.5f)
            val serverUrl by prefs.serverUrl.collectAsState(initial = "")
            val scope = rememberCoroutineScope()

            var appState by remember { mutableStateOf(AppState.WELCOME) }
            var isLoggedIn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isLoggedIn = prefs.serverUrl.first().isNotEmpty() &&
                             prefs.apiKey.first().isNotEmpty()
                val hasSeenWelcome = prefs.hasSeenWelcome.first()
                appState = when {
                    isLoggedIn -> AppState.MAIN
                    hasSeenWelcome -> AppState.LOGIN
                    else -> AppState.WELCOME
                }
            }

            BookNextTheme(
                themeId = themeId,
                darkTheme = darkMode,
                uiFontScale = uiFontScale,
                uiFontFamily = uiFontFamily,
                uiLineSpacing = uiLineSpacing,
            ) {
                CompositionLocalProvider(LocalActivity provides this@MainActivity) {
                    when (appState) {
                        AppState.WELCOME -> com.booknext.app.ui.welcome.WelcomeScreen(
                            onEnterLocal = {
                                scope.launch { prefs.setHasSeenWelcome(true) }
                                appState = AppState.MAIN
                            },
                            onLogin = {
                                scope.launch { prefs.setHasSeenWelcome(true) }
                                appState = AppState.LOGIN
                            },
                        )
                        AppState.LOGIN -> com.booknext.app.ui.login.LoginScreen(
                            onLoginSuccess = { appState = AppState.MAIN },
                            onBack = { appState = AppState.WELCOME },
                        )
                        AppState.MAIN -> MainDrawerScaffold(
                            isLoggedIn = isLoggedIn || appState != AppState.WELCOME,
                            onLoginRequest = { appState = AppState.LOGIN },
                            serverUrl = serverUrl,
                            isDarkMode = darkMode,
                            onDarkModeToggle = {
                                scope.launch { prefs.saveDarkMode(!darkMode) }
                            },
                            onLogout = {
                                scope.launch {
                                    prefs.clear()
                                    recreate()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDrawerScaffold(
    isLoggedIn: Boolean,
    onLoginRequest: () -> Unit,
    serverUrl: String,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
    onLogout: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(DrawerPage.BOOKSHELF) }
    var readerBookId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showQuotes by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    BackHandler(enabled = readerBookId != null || showSettings || showQuotes || showStats || showFavoritesOnly || showBookmarks || drawerState.isOpen) {
        when {
            readerBookId != null -> readerBookId = null
            showQuotes -> showQuotes = false
            showStats -> showStats = false
            showBookmarks -> showBookmarks = false
            showSettings -> showSettings = false
            showFavoritesOnly -> showFavoritesOnly = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    if (showQuotes) {
        com.booknext.app.ui.quotes.QuotesScreen(
            onBack = { showQuotes = false },
        )
        return
    }

    if (showStats) {
        com.booknext.app.ui.stats.StatsScreen(
            onBack = { showStats = false },
        )
        return
    }

    if (showBookmarks) {
        com.booknext.app.ui.bookmarks.BookmarksScreen(
            onBack = { showBookmarks = false },
            onBookClick = { readerBookId = it },
        )
        return
    }

    if (showSettings) {
        com.booknext.app.ui.settings.SettingsScreen(
            onBack = { showSettings = false },
            onLogout = {
                showSettings = false
                scope.launch { drawerState.close() }
                onLogout()
            },
        )
        return
    }

    if (readerBookId != null) {
        com.booknext.app.ui.reader.ReaderScreen(
            bookId = readerBookId!!,
            onBack = { readerBookId = null },
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentPage = currentPage,
                serverUrl = serverUrl,
                isLoggedIn = isLoggedIn,
                onLoginClick = {
                    scope.launch { drawerState.close() }
                    onLoginRequest()
                },
                onPageSelect = { page ->
                    currentPage = page
                    scope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    showSettings = true
                },
                onDarkModeToggle = onDarkModeToggle,
                isDarkMode = isDarkMode,
                onLogout = onLogout,
            )
        },
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "page_switch",
        ) { page ->
            when (page) {
                DrawerPage.RECENT -> RecentScreen(
                    onBookClick = { readerBookId = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToNotes = { showQuotes = true },
                    onNavigateToStats = { showStats = true },
                    onNavigateToFavorites = {
                        currentPage = DrawerPage.BOOKSHELF
                        showFavoritesOnly = true
                    },
                    onNavigateToBookmarks = { showBookmarks = true },
                )
                DrawerPage.BOOKSHELF -> BookshelfScreen(
                    onBookClick = { readerBookId = it.bookId },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onUploadClick = { currentPage = DrawerPage.CLOUD },
                    showFavoritesOnly = showFavoritesOnly,
                    onFavoritesFilterCleared = { showFavoritesOnly = false },
                )
                DrawerPage.CATEGORY -> CategoryScreen(
                    onBookClick = { readerBookId = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
                DrawerPage.LOCAL -> LocalScreen(
                    onBookClick = { readerBookId = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
                DrawerPage.CLOUD -> CloudScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBookClick = { readerBookId = it },
                )
                DrawerPage.ONLINE_LIBRARY -> OnlineLibraryScreen(
                    onBack = { currentPage = DrawerPage.RECENT },
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            }
        }
    }
}
