package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesManager
import com.example.data.model.FileExplorerLayout
import com.example.data.model.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Code0", appName)
    }

    @Test
    fun testPreferencesManagerDefaultsAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        // Default theme must be CHARCOAL_GREEN as requested
        assertEquals(ThemePreset.CHARCOAL_GREEN, prefs.themePreset.value)
        assertEquals(FileExplorerLayout.EXPANDABLE_TREE, prefs.explorerLayout.value)
        assertFalse(prefs.isFirstLaunchCompleted.value)

        // Change and verify persistence
        prefs.setThemePreset(ThemePreset.BLACK_ORANGE)
        assertEquals(ThemePreset.BLACK_ORANGE, prefs.themePreset.value)

        prefs.setExplorerLayout(ThemePreset.BLACK_ORANGE.let { FileExplorerLayout.COMBINED })
        assertEquals(FileExplorerLayout.COMBINED, prefs.explorerLayout.value)

        prefs.setFirstLaunchCompleted(true)
        assertTrue(prefs.isFirstLaunchCompleted.value)

        // Credentials
        prefs.setGitHubCredentials("ghp_test12345", "testuser")
        assertEquals("ghp_test12345", prefs.githubToken.value)
        assertEquals("testuser", prefs.githubUsername.value)
    }
}
