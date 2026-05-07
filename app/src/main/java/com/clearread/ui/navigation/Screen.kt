package com.clearread.ui.navigation

/**
 * Defines all navigation routes in the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explorer : Screen("explorer")
    data object Reader : Screen("reader/{fileUri}?page={page}") {
        fun createRoute(fileUri: String, page: Int = -1): String = "reader/$fileUri?page=$page"
    }
    data object Bookmarks : Screen("bookmarks")
    data object Settings : Screen("settings")
}
