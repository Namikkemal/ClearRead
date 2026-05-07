package com.clearread.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app preferences using SharedPreferences.
 * Exposes reactive StateFlows for Compose observation.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("clearread_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SCROLL_DIRECTION = "scroll_direction"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_AMOLED_MODE = "amoled_mode"
        private const val KEY_RECENT_FOLDER_URI = "recent_folder_uri"
        private const val KEY_RECENT_FOLDER_NAME = "recent_folder_name"

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        const val SCROLL_VERTICAL = 0
        const val SCROLL_HORIZONTAL = 1

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // --- Theme Mode ---
    private val _themeMode = MutableStateFlow(prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    // --- Scroll Direction ---
    private val _scrollDirection = MutableStateFlow(prefs.getInt(KEY_SCROLL_DIRECTION, SCROLL_VERTICAL))
    val scrollDirection: StateFlow<Int> = _scrollDirection.asStateFlow()

    fun setScrollDirection(direction: Int) {
        prefs.edit().putInt(KEY_SCROLL_DIRECTION, direction).apply()
        _scrollDirection.value = direction
    }

    // --- Keep Screen On ---
    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    // --- AMOLED Mode ---
    private val _amoledMode = MutableStateFlow(prefs.getBoolean(KEY_AMOLED_MODE, false))
    val amoledMode: StateFlow<Boolean> = _amoledMode.asStateFlow()

    fun setAmoledMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED_MODE, enabled).apply()
        _amoledMode.value = enabled
    }

    // --- Recent Folder ---
    fun saveRecentFolder(uri: String, name: String) {
        prefs.edit()
            .putString(KEY_RECENT_FOLDER_URI, uri)
            .putString(KEY_RECENT_FOLDER_NAME, name)
            .apply()
    }

    fun getRecentFolderUri(): String? = prefs.getString(KEY_RECENT_FOLDER_URI, null)
    fun getRecentFolderName(): String? = prefs.getString(KEY_RECENT_FOLDER_NAME, null)

    fun clearRecentFolder() {
        prefs.edit()
            .remove(KEY_RECENT_FOLDER_URI)
            .remove(KEY_RECENT_FOLDER_NAME)
            .apply()
    }
}
