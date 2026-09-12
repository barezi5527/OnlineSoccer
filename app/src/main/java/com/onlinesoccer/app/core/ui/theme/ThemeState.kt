package com.onlinesoccer.app.core.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** M10: Manueller Hell/Dunkel-Schalter (übersteuert optional die Systemeinstellung). */
class ThemeState(initialDark: Boolean?) {
    var dark by mutableStateOf(initialDark ?: false)
    var followSystem by mutableStateOf(initialDark == null)
        private set

    fun setManual(darkValue: Boolean) {
        dark = darkValue
        followSystem = false
    }

    fun setSystem() {
        followSystem = true
    }
}

@Composable
fun rememberThemeState(initialDark: Boolean?): ThemeState {
    return remember { ThemeState(initialDark) }
}

fun persistTheme(context: Context, state: ThemeState) {
    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    if (state.followSystem) {
        prefs.edit().remove("dark_mode").apply()
    } else {
        prefs.edit().putBoolean("dark_mode", state.dark).apply()
    }
}

fun loadThemeOverride(context: Context): Boolean? {
    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    return if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
}