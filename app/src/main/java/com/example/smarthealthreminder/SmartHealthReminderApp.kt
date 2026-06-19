package com.example.smarthealthreminder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.features.settings.SettingsActivity

class SmartHealthReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(SettingsActivity.getSavedNightMode(this))
    }
}
