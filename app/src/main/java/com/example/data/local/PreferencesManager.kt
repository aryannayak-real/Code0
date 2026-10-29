package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.FileExplorerLayout
import com.example.data.model.ThemeMode
import com.example.data.model.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("code0_prefs", Context.MODE_PRIVATE)

    private val _themePreset = MutableStateFlow(loadThemePreset())
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _explorerLayout = MutableStateFlow(loadExplorerLayout())
    val explorerLayout: StateFlow<FileExplorerLayout> = _explorerLayout.asStateFlow()

    private val _isFirstLaunchCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_FIRST_LAUNCH, false)
    )
    val isFirstLaunchCompleted: StateFlow<Boolean> = _isFirstLaunchCompleted.asStateFlow()

    private val _githubToken = MutableStateFlow(prefs.getString(KEY_GH_TOKEN, null))
    val githubToken: StateFlow<String?> = _githubToken.asStateFlow()

    private val _githubUsername = MutableStateFlow(prefs.getString(KEY_GH_USER, null))
    val githubUsername: StateFlow<String?> = _githubUsername.asStateFlow()

    private val _lastActiveProjectId = MutableStateFlow(prefs.getString(KEY_LAST_PROJECT, null))
    val lastActiveProjectId: StateFlow<String?> = _lastActiveProjectId.asStateFlow()

    private fun loadThemePreset(): ThemePreset {
        val name = prefs.getString(KEY_THEME_PRESET, ThemePreset.CHARCOAL_GREEN.name)
        return try {
            ThemePreset.valueOf(name ?: ThemePreset.CHARCOAL_GREEN.name)
        } catch (_: Exception) {
            ThemePreset.CHARCOAL_GREEN
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.DARK.name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadExplorerLayout(): FileExplorerLayout {
        val name = prefs.getString(KEY_EXPLORER_LAYOUT, FileExplorerLayout.EXPANDABLE_TREE.name)
        return try {
            FileExplorerLayout.valueOf(name ?: FileExplorerLayout.EXPANDABLE_TREE.name)
        } catch (_: Exception) {
            FileExplorerLayout.EXPANDABLE_TREE
        }
    }

    fun setThemePreset(preset: ThemePreset) {
        prefs.edit().putString(KEY_THEME_PRESET, preset.name).apply()
        _themePreset.value = preset
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setExplorerLayout(layout: FileExplorerLayout) {
        prefs.edit().putString(KEY_EXPLORER_LAYOUT, layout.name).apply()
        _explorerLayout.value = layout
    }

    fun setFirstLaunchCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, completed).apply()
        _isFirstLaunchCompleted.value = completed
    }

    fun setGitHubCredentials(token: String?, username: String?) {
        prefs.edit()
            .putString(KEY_GH_TOKEN, token)
            .putString(KEY_GH_USER, username)
            .apply()
        _githubToken.value = token
        _githubUsername.value = username
    }

    fun setLastActiveProjectId(id: String?) {
        prefs.edit().putString(KEY_LAST_PROJECT, id).apply()
        _lastActiveProjectId.value = id
    }

    fun resetToDefaults() {
        setThemePreset(ThemePreset.CHARCOAL_GREEN)
        setThemeMode(ThemeMode.DARK)
        setExplorerLayout(FileExplorerLayout.EXPANDABLE_TREE)
        setFirstLaunchCompleted(false)
    }

    companion object {
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_EXPLORER_LAYOUT = "explorer_layout"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
        private const val KEY_GH_TOKEN = "github_token"
        private const val KEY_GH_USER = "github_username"
        private const val KEY_LAST_PROJECT = "last_active_project"
    }
}
