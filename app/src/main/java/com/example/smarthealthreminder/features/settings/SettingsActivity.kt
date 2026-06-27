package com.example.smarthealthreminder.features.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.example.smarthealthreminder.features.welcome.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "smart_health_settings"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_EARLY_REMINDERS = "early_reminders_enabled"
        const val KEY_ALARM_SNOOZE_MINUTES = "alarm_snooze_minutes"
        const val KEY_REMINDER_SNOOZE_MINUTES = "reminder_snooze_minutes"
        const val DEFAULT_SNOOZE_MINUTES = 15
        const val MIN_SNOOZE_MINUTES = 1
        const val MAX_SNOOZE_MINUTES = 120
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
        private const val REQUEST_NOTIFICATIONS = 3001

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

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var switchEarlyReminders: SwitchCompat
    private lateinit var themeModeSpinner: Spinner
    private lateinit var tvAlarmSnoozeValue: TextView
    private lateinit var tvReminderSnoozeValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(getSavedNightMode(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadValues()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (::switchNotifications.isInitialized) {
            switchNotifications.setOnCheckedChangeListener(null)
            switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS, true) && areNotificationsAllowed()
            setupNotificationSwitchListener()
        }
    }

    private fun bindViews() {
        switchNotifications = findViewById(R.id.switch_notifications)
        switchVibration = findViewById(R.id.switch_vibration)
        switchEarlyReminders = findViewById(R.id.switch_early_reminders)
        themeModeSpinner = findViewById(R.id.spinner_theme_mode)
        tvAlarmSnoozeValue = findViewById(R.id.tv_alarm_snooze_value)
        tvReminderSnoozeValue = findViewById(R.id.tv_reminder_snooze_value)
    }

    private fun loadValues() {
        switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS, true) && areNotificationsAllowed()
        switchVibration.isChecked = prefs.getBoolean(KEY_VIBRATION, true)
        switchEarlyReminders.isChecked = prefs.getBoolean(KEY_EARLY_REMINDERS, true)
        themeModeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Light", "Dark", "Same as device")
        )
        themeModeSpinner.setSelection(themeModeToPosition(prefs.getString(KEY_THEME_MODE, THEME_LIGHT)))
        updateSnoozeValueLabels()
    }

    private fun updateSnoozeValueLabels() {
        tvAlarmSnoozeValue.text = formatSnoozeMinutes(getAlarmSnoozeMinutes(this))
        tvReminderSnoozeValue.text = formatSnoozeMinutes(getReminderSnoozeMinutes(this))
    }

    private fun formatSnoozeMinutes(minutes: Int): String {
        return getString(R.string.settings_snooze_minutes_value, minutes)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        setupNotificationSwitchListener()
        setupBottomNavigation()

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply()
        }

        switchEarlyReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_EARLY_REMINDERS, isChecked).apply()
        }

        themeModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val themeMode = positionToThemeMode(position)
                if (prefs.getString(KEY_THEME_MODE, THEME_LIGHT) == themeMode) return

                prefs.edit()
                    .putString(KEY_THEME_MODE, themeMode)
                    .putBoolean(KEY_DARK_MODE, themeMode == THEME_DARK)
                    .apply()
                AppCompatDelegate.setDefaultNightMode(getSavedNightMode(this@SettingsActivity))
                recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<LinearLayout>(R.id.row_exact_alarm).setOnClickListener {
            openExactAlarmSettings()
        }

        findViewById<LinearLayout>(R.id.row_alarm_snooze).setOnClickListener {
            showSnoozeDurationDialog(
                title = getString(R.string.settings_alarm_snooze_title),
                currentMinutes = getAlarmSnoozeMinutes(this),
                prefKey = KEY_ALARM_SNOOZE_MINUTES,
                valueView = tvAlarmSnoozeValue
            )
        }

        findViewById<LinearLayout>(R.id.row_reminder_snooze).setOnClickListener {
            showSnoozeDurationDialog(
                title = getString(R.string.settings_reminder_snooze_title),
                currentMinutes = getReminderSnoozeMinutes(this),
                prefKey = KEY_REMINDER_SNOOZE_MINUTES,
                valueView = tvReminderSnoozeValue
            )
        }

        findViewById<LinearLayout>(R.id.row_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.row_reports).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            confirmLogout()
        }
    }

    private fun setupNotificationSwitchListener() {
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply()
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            } else {
                openAppNotificationSettings()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (!areNotificationsAllowed()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    private fun areNotificationsAllowed(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        } else {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            } else {
                Toast.makeText(this, "Exact alarm permission is already allowed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Exact alarm permission is already available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSnoozeDurationDialog(
        title: String,
        currentMinutes: Int,
        prefKey: String,
        valueView: TextView
    ) {
        val picker = NumberPicker(this).apply {
            minValue = MIN_SNOOZE_MINUTES
            maxValue = MAX_SNOOZE_MINUTES
            value = currentMinutes
            wrapSelectorWheel = false
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(picker)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val minutes = picker.value.coerceIn(MIN_SNOOZE_MINUTES, MAX_SNOOZE_MINUTES)
                prefs.edit().putInt(prefKey, minutes).apply()
                valueView.text = formatSnoozeMinutes(minutes)
                Toast.makeText(
                    this,
                    getString(R.string.settings_snooze_saved, minutes),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Do you want to log out of Smart Health Reminder?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        BottomNavHelper.setup(
            activity = this,
            bottomNavigation = bottomNav,
            selectedItemId = R.id.action_settings
        )
    }

    private fun themeModeToPosition(themeMode: String?): Int {
        return when (themeMode) {
            THEME_DARK -> 1
            THEME_SYSTEM -> 2
            else -> 0
        }
    }

    private fun positionToThemeMode(position: Int): String {
        return when (position) {
            1 -> THEME_DARK
            2 -> THEME_SYSTEM
            else -> THEME_LIGHT
        }
    }
}
