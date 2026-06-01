package com.booknext.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.booknext.app.ui.bookshelf.BookshelfScreen
import com.booknext.app.ui.login.LoginScreen
import com.booknext.app.ui.reader.ReaderScreen
import com.booknext.app.ui.settings.SettingsScreen
import com.booknext.app.ui.upload.UploadScreen
import com.booknext.app.ui.quotes.QuotesScreen
import com.booknext.app.ui.stats.StatsScreen

object Routes {
    const val LOGIN = "login"
    const val BOOKSHELF = "bookshelf"
    const val READER = "reader/{bookId}"
    const val SETTINGS = "settings"
    const val UPLOAD = "upload"
    const val QUOTES = "quotes"
    const val STATS = "stats"
    fun reader(bookId: String) = "reader/$bookId"
}

@Composable
fun NavGraph(startDestination: String = Routes.LOGIN) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.BOOKSHELF) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.BOOKSHELF) {
            BookshelfScreen(
                onBookClick = { book -> navController.navigate(Routes.reader(book.bookId)) },
                onMenuClick = { navController.navigate(Routes.SETTINGS) },
                onUploadClick = { navController.navigate(Routes.UPLOAD) },
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStack ->
            val bookId = backStack.arguments?.getString("bookId") ?: return@composable
            ReaderScreen(bookId = bookId, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
    composable(Routes.UPLOAD) {
            UploadScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.QUOTES) {
            QuotesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
    }
}
