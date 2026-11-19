package com.oleh11.vatrasmoker.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Light / Dark / System choice, stored on the phone only. */
object ThemeSettings {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(context: Context) {
        val saved = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme_mode", null)
        mode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("theme_mode", newMode.name).apply()
    }
}
