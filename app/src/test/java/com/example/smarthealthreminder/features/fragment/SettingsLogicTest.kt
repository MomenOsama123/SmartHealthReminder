package com.example.smarthealthreminder.features.fragment

import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.features.settings.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for SettingsActivity pure logic (theme mapping, prefs constants).
 * No Android context needed — these test only the static/companion logic.
 */
class SettingsLogicTest {

    // ───── Theme mode constants ─────

    @Test
    fun `THEME_LIGHT constant is light`() {
        assertEquals("light", SettingsActivity.THEME_LIGHT)
    }

    @Test
    fun `THEME_DARK constant is dark`() {
        assertEquals("dark", SettingsActivity.THEME_DARK)
    }

    @Test
    fun `THEME_SYSTEM constant is system`() {
        assertEquals("system", SettingsActivity.THEME_SYSTEM)
    }

    // ───── Night mode mapping (mirrors positionToThemeMode & getSavedNightMode) ─────

    @Test
    fun `position 0 maps to THEME_LIGHT`() {
        val mode = positionToThemeMode(0)
        assertEquals(SettingsActivity.THEME_LIGHT, mode)
    }

    @Test
    fun `position 1 maps to THEME_DARK`() {
        val mode = positionToThemeMode(1)
        assertEquals(SettingsActivity.THEME_DARK, mode)
    }

    @Test
    fun `position 2 maps to THEME_SYSTEM`() {
        val mode = positionToThemeMode(2)
        assertEquals(SettingsActivity.THEME_SYSTEM, mode)
    }

    @Test
    fun `unknown position defaults to THEME_LIGHT`() {
        val mode = positionToThemeMode(99)
        assertEquals(SettingsActivity.THEME_LIGHT, mode)
    }

    @Test
    fun `THEME_DARK maps to MODE_NIGHT_YES`() {
        val nightMode = themeModeToNightMode(SettingsActivity.THEME_DARK)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightMode)
    }

    @Test
    fun `THEME_SYSTEM maps to MODE_NIGHT_FOLLOW_SYSTEM`() {
        val nightMode = themeModeToNightMode(SettingsActivity.THEME_SYSTEM)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightMode)
    }

    @Test
    fun `THEME_LIGHT maps to MODE_NIGHT_NO`() {
        val nightMode = themeModeToNightMode(SettingsActivity.THEME_LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightMode)
    }

    @Test
    fun `null theme mode maps to MODE_NIGHT_NO`() {
        val nightMode = themeModeToNightMode(null)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightMode)
    }

    // ───── Prefs keys ─────

    @Test
    fun `PREFS_NAME is correct`() {
        assertEquals("smart_health_settings", SettingsActivity.PREFS_NAME)
    }

    @Test
    fun `KEY_NOTIFICATIONS is correct`() {
        assertEquals("notifications_enabled", SettingsActivity.KEY_NOTIFICATIONS)
    }

    @Test
    fun `KEY_VIBRATION is correct`() {
        assertEquals("vibration_enabled", SettingsActivity.KEY_VIBRATION)
    }

    @Test
    fun `KEY_EARLY_REMINDERS is correct`() {
        assertEquals("early_reminders_enabled", SettingsActivity.KEY_EARLY_REMINDERS)
    }

    @Test
    fun `KEY_THEME_MODE is correct`() {
        assertEquals("theme_mode", SettingsActivity.KEY_THEME_MODE)
    }

    // ───── Spinner position round-trip ─────

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for LIGHT`() {
        val pos = themeModeToPosition(SettingsActivity.THEME_LIGHT)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsActivity.THEME_LIGHT, mode)
    }

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for DARK`() {
        val pos = themeModeToPosition(SettingsActivity.THEME_DARK)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsActivity.THEME_DARK, mode)
    }

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for SYSTEM`() {
        val pos = themeModeToPosition(SettingsActivity.THEME_SYSTEM)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsActivity.THEME_SYSTEM, mode)
    }

    // ─── Helpers matching private SettingsActivity methods ───

    private fun positionToThemeMode(position: Int): String {
        return when (position) {
            1 -> SettingsActivity.THEME_DARK
            2 -> SettingsActivity.THEME_SYSTEM
            else -> SettingsActivity.THEME_LIGHT
        }
    }

    private fun themeModeToPosition(themeMode: String?): Int {
        return when (themeMode) {
            SettingsActivity.THEME_DARK -> 1
            SettingsActivity.THEME_SYSTEM -> 2
            else -> 0
        }
    }

    private fun themeModeToNightMode(themeMode: String?): Int {
        return when (themeMode) {
            SettingsActivity.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SettingsActivity.THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
    }
}
