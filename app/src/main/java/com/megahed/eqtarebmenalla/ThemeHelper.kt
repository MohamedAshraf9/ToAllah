package com.megahed.eqtarebmenalla

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val DARK_MODE_PREF = "pref_dark_mode"

    fun applyTheme(settings: SharedPreferences) {
        val darkModeEnabled = settings.getBoolean(DARK_MODE_PREF, true)
        val mode = if (darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isDarkModeEnabled(settings: SharedPreferences): Boolean {
        return settings.getBoolean(DARK_MODE_PREF, true)
    }

    fun setupThemeChangeListener(context: Context, listener: (Boolean) -> Unit = {}): SharedPreferences.OnSharedPreferenceChangeListener {
        val prefs = context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )

        val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == DARK_MODE_PREF) {
                val isDarkMode = sharedPreferences.getBoolean(key, true)
                listener(isDarkMode)
                applyTheme(sharedPreferences)
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(changeListener)
        return changeListener
    }
}