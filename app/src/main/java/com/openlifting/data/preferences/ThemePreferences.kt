package com.openlifting.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.openlifting.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed persistence for the user's theme preference.
 *
 * Exposed as a [StateFlow] so Composables can react to changes immediately. Backed by a
 * SharedPreferences file that survives app restarts. Kept minimal on purpose — when more
 * preferences appear (language, notifications, etc.) we can swap to DataStore Preferences.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(load())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private fun load(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    companion object {
        private const val PREFS_NAME      = "openlifting_prefs"
        private const val KEY_THEME_MODE  = "theme_mode"
    }
}
