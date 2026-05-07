package com.clearread

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.rememberNavController
import com.clearread.data.local.PreferencesManager
import com.clearread.ui.navigation.ClearReadNavGraph
import com.clearread.ui.theme.ClearReadTheme


class MainActivity : AppCompatActivity() {

    private val prefs by lazy { PreferencesManager.getInstance(applicationContext) }
    private var intentUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial intent handling for external files
        intentUri = handleIntent(intent)

        // Modern edge-to-edge system
        enableEdgeToEdge()

        setContent {
            // Reactive state collection for theme and amoled settings
            val themeMode by prefs.themeMode.collectAsState()
            val amoledMode by prefs.amoledMode.collectAsState()
            
            val isDarkTheme = when (themeMode) {
                PreferencesManager.THEME_LIGHT -> false
                PreferencesManager.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            ClearReadTheme(darkTheme = isDarkTheme, amoledMode = amoledMode) {
                // Logic to trigger smooth transition on language change
                val config = LocalConfiguration.current
                val localeKey = remember(config) { 
                    config.locales.get(0)?.toLanguageTag() ?: "en"
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Crossfade ensures smooth UI transition when language changes
                    Crossfade(
                        targetState = localeKey,
                        animationSpec = tween(durationMillis = 600),
                        label = "LocaleTransition"
                    ) { _ ->
                        ClearReadNavGraph(
                            navController = navController, 
                            initialUri = intentUri,
                            onIntentHandled = { intentUri = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentUri = handleIntent(intent)
    }

    /**
     * Safely processes incoming file intents and takes persistable URI permissions.
     */
    private fun handleIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        
        if (intent.action == Intent.ACTION_VIEW) {
            try {
                // Only take persistable permissions for content URIs
                if (data.scheme == "content") {
                    contentResolver.takePersistableUriPermission(
                        data,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: SecurityException) {
                // Occurs if the file provider doesn't support persistable permissions
            } catch (e: Exception) {
                // Generic fallback
            }
            return data.toString()
        }
        return null
    }
}
