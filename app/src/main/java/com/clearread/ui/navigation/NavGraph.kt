package com.clearread.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clearread.ui.bookmarks.BookmarksScreen
import com.clearread.ui.explorer.FileExplorerScreen
import com.clearread.ui.home.HomeScreen
import com.clearread.ui.reader.PdfReaderScreen
import com.clearread.ui.settings.SettingsScreen

/**
 * Main navigation graph for ClearRead — all screens wired together.
 */
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ClearReadNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    initialUri: String? = null,
    onIntentHandled: () -> Unit = {}
) {
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            val encoded = Uri.encode(initialUri)
            navController.navigate(Screen.Reader.createRoute(encoded))
            onIntentHandled()
        }
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ── Home Screen ──
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToReader = { fileUri, page ->
                    val encoded = Uri.encode(fileUri)
                    navController.navigate(Screen.Reader.createRoute(encoded, page))
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onNavigateToExplorer = {
                    navController.navigate(Screen.Explorer.route)
                }
            )
        }

        // ── PDF Reader Screen ──
        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("fileUri") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val fileUri = backStackEntry.arguments?.getString("fileUri") ?: ""
            val page = backStackEntry.arguments?.getInt("page") ?: 0
            PdfReaderScreen(
                fileUri = fileUri,
                initialPage = page,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── File Explorer Screen ──
        composable(Screen.Explorer.route) {
            FileExplorerScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenFile = { fileUri ->
                    val encoded = Uri.encode(fileUri)
                    navController.navigate(Screen.Reader.createRoute(encoded, -1))
                }
            )
        }

        // ── Bookmarks Screen ──
        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReader = { filePath, page ->
                    val encoded = Uri.encode(filePath)
                    navController.navigate(Screen.Reader.createRoute(encoded, page))
                }
            )
        }

        // ── Settings Screen ──
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
