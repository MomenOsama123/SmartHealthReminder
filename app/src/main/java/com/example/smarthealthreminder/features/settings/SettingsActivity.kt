package com.example.smarthealthreminder.features.settings

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    companion object {
        const val PREFS_NAME = "smart_health_settings"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_EARLY_REMINDERS = "early_reminders_enabled"
        const val KEY_ALARM_SNOOZE_MINUTES = "alarm_snooze_minutes"
        const val KEY_REMINDER_SNOOZE_MINUTES = "reminder_snooze_minutes"
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val MIN_SNOOZE_MINUTES = 1
        const val MAX_SNOOZE_MINUTES = 120
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
        const val REQUEST_NOTIFICATIONS = 3001

        fun getSavedNightMode(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedMode = prefs.getString(KEY_THEME_MODE, null)
                ?: if (prefs.getBoolean(KEY_DARK_MODE, false)) THEME_DARK else THEME_LIGHT

            return when (savedMode) {
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        }

        fun getAlarmSnoozeMinutes(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_ALARM_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
                .coerceIn(MIN_SNOOZE_MINUTES, MAX_SNOOZE_MINUTES)
        }

        fun getReminderSnoozeMinutes(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_REMINDER_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
                .coerceIn(MIN_SNOOZE_MINUTES, MAX_SNOOZE_MINUTES)
        }
    }
}
