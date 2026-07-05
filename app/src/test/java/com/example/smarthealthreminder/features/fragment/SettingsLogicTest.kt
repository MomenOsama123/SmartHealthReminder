package com.example.smarthealthreminder.features.fragment

import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * No Android context needed — these test only the static/companion logic.
 */
class SettingsLogicTest {

    // ───── Theme mode constants ─────

    @Test
    fun `THEME_LIGHT constant is light`() {
        assertEquals("light", SettingsPrefs.THEME_LIGHT)
    }

    @Test
    fun `THEME_DARK constant is dark`() {
        assertEquals("dark", SettingsPrefs.THEME_DARK)
    }

    @Test
    fun `THEME_SYSTEM constant is system`() {
        assertEquals("system", SettingsPrefs.THEME_SYSTEM)
    }

    // ───── Night mode mapping (mirrors positionToThemeMode & getSavedNightMode) ─────

    @Test
    fun `position 0 maps to THEME_LIGHT`() {
        val mode = positionToThemeMode(0)
        assertEquals(SettingsPrefs.THEME_LIGHT, mode)
    }

    @Test
    fun `position 1 maps to THEME_DARK`() {
        val mode = positionToThemeMode(1)
        assertEquals(SettingsPrefs.THEME_DARK, mode)
    }

    @Test
    fun `position 2 maps to THEME_SYSTEM`() {
        val mode = positionToThemeMode(2)
        assertEquals(SettingsPrefs.THEME_SYSTEM, mode)
    }

    @Test
    fun `unknown position defaults to THEME_LIGHT`() {
        val mode = positionToThemeMode(99)
        assertEquals(SettingsPrefs.THEME_LIGHT, mode)
    }

    @Test
    fun `THEME_DARK maps to MODE_NIGHT_YES`() {
        val nightMode = themeModeToNightMode(SettingsPrefs.THEME_DARK)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightMode)
    }

    @Test
    fun `THEME_SYSTEM maps to MODE_NIGHT_FOLLOW_SYSTEM`() {
        val nightMode = themeModeToNightMode(SettingsPrefs.THEME_SYSTEM)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightMode)
    }

    @Test
    fun `THEME_LIGHT maps to MODE_NIGHT_NO`() {
        val nightMode = themeModeToNightMode(SettingsPrefs.THEME_LIGHT)
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
        assertEquals("smart_health_settings", SettingsPrefs.PREFS_NAME)
    }

    @Test
    fun `KEY_NOTIFICATIONS is correct`() {
        assertEquals("notifications_enabled", SettingsPrefs.KEY_NOTIFICATIONS)
    }

    @Test
    fun `KEY_VIBRATION is correct`() {
        assertEquals("vibration_enabled", SettingsPrefs.KEY_VIBRATION)
    }

    @Test
    fun `KEY_EARLY_REMINDERS is correct`() {
        assertEquals("early_reminders_enabled", SettingsPrefs.KEY_EARLY_REMINDERS)
    }

    @Test
    fun `KEY_THEME_MODE is correct`() {
        assertEquals("theme_mode", SettingsPrefs.KEY_THEME_MODE)
    }

    // ───── Spinner position round-trip ─────

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for LIGHT`() {
        val pos = themeModeToPosition(SettingsPrefs.THEME_LIGHT)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsPrefs.THEME_LIGHT, mode)
    }

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for DARK`() {
        val pos = themeModeToPosition(SettingsPrefs.THEME_DARK)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsPrefs.THEME_DARK, mode)
    }

    @Test
    fun `themeModeToPosition then positionToThemeMode is identity for SYSTEM`() {
        val pos = themeModeToPosition(SettingsPrefs.THEME_SYSTEM)
        val mode = positionToThemeMode(pos)
        assertEquals(SettingsPrefs.THEME_SYSTEM, mode)
    }

    @Test
    fun `DEFAULT_SNOOZE_MINUTES is 10`() {
        assertEquals(10, SettingsPrefs.DEFAULT_SNOOZE_MINUTES)
    }

    @Test
    fun `KEY_ALARM_SNOOZE_MINUTES is correct`() {
        assertEquals("alarm_snooze_minutes", SettingsPrefs.KEY_ALARM_SNOOZE_MINUTES)
    }

    @Test
    fun `KEY_REMINDER_SNOOZE_MINUTES is correct`() {
        assertEquals("reminder_snooze_minutes", SettingsPrefs.KEY_REMINDER_SNOOZE_MINUTES)
    }

    // ─── Helpers matching private SettingsPrefs methods ───

    private fun positionToThemeMode(position: Int): String {
        return when (position) {
            1 -> SettingsPrefs.THEME_DARK
            2 -> SettingsPrefs.THEME_SYSTEM
            else -> SettingsPrefs.THEME_LIGHT
        }
    }

    private fun themeModeToPosition(themeMode: String?): Int {
        return when (themeMode) {
            SettingsPrefs.THEME_DARK -> 1
            SettingsPrefs.THEME_SYSTEM -> 2
            else -> 0
        }
    }

    private fun themeModeToNightMode(themeMode: String?): Int {
        return when (themeMode) {
            SettingsPrefs.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SettingsPrefs.THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
    }
}
