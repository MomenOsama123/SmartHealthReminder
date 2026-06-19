package com.example.smarthealthreminder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.features.settings.SettingsActivity

class SmartHealthReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val darkModeEnabled = getSharedPreferences(
            SettingsActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getBoolean(SettingsActivity.KEY_DARK_MODE, false)

        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
