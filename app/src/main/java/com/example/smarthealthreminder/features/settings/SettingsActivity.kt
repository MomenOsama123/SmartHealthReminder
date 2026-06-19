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
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
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
import com.example.smarthealthreminder.features.welcome.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "smart_health_settings"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_EARLY_REMINDERS = "early_reminders_enabled"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val REQUEST_NOTIFICATIONS = 3001
    }

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var switchEarlyReminders: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedDarkMode()
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
            switchNotifications.isChecked = areNotificationsAllowed()
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, switchNotifications.isChecked).apply()
            setupNotificationSwitchListener()
        }
    }

    private fun bindViews() {
        switchNotifications = findViewById(R.id.switch_notifications)
        switchVibration = findViewById(R.id.switch_vibration)
        switchEarlyReminders = findViewById(R.id.switch_early_reminders)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
    }

    private fun loadValues() {
        switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        switchVibration.isChecked = prefs.getBoolean(KEY_VIBRATION, true)
        switchEarlyReminders.isChecked = prefs.getBoolean(KEY_EARLY_REMINDERS, true)
        switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        setupNotificationSwitchListener()

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply()
        }

        switchEarlyReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_EARLY_REMINDERS, isChecked).apply()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }

        findViewById<LinearLayout>(R.id.row_exact_alarm).setOnClickListener {
            openExactAlarmSettings()
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

    private fun applySavedDarkMode() {
        val darkModeEnabled = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
