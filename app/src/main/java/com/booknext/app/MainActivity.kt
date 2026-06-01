package com.booknext.app

import android.os.Bundle
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
import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.ui.NavGraph
import com.booknext.app.ui.Routes
import com.booknext.app.ui.bookshelf.BookshelfScreen
import com.booknext.app.ui.category.CategoryScreen
import com.booknext.app.ui.cloud.CloudScreen
import com.booknext.app.ui.common.BookNextTheme
import com.booknext.app.ui.drawer.DrawerContent
import com.booknext.app.ui.drawer.DrawerPage
import com.booknext.app.ui.local.LocalScreen
import com.booknext.app.ui.recent.RecentScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val isLoggedIn = runBlocking {
            prefs.serverUrl.first().isNotEmpty() && prefs.apiKey.first().isNotEmpty()
        }
        val hasSeenWelcome = runBlocking { prefs.hasSeenWelcome.first() }

        setContent {
            val themeId by prefs.themeId.collectAsState(initial = "blue")
            val darkMode by prefs.darkMode.collectAsState(initial = false)
            val serverUrl by prefs.serverUrl.collectAsState(initial = "")
            val scope = rememberCoroutineScope()

            var appState by remember {
                mutableStateOf(
                    when {
                        isLoggedIn -> AppState.MAIN
                        hasSeenWelcome -> AppState.MAIN
                        else -> AppState.WELCOME
                    }
                )
            }

            BookNextTheme(themeId = themeId, darkTheme = darkMode) {
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
                )
                DrawerPage.BOOKSHELF -> BookshelfScreen(
                    onBookClick = { readerBookId = it.bookId },
                    onMenuClick = { scope.launch { drawerState.open() } },
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
            }
        }
    }
}
