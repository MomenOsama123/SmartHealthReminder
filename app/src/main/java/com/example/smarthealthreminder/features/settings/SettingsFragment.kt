package com.example.smarthealthreminder.features.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.FragmentSettingsBinding
import com.example.smarthealthreminder.features.welcome.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit
import androidx.core.net.toUri

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        loadValues()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = prefs.getBoolean(SettingsPrefs.KEY_NOTIFICATIONS, true) && areNotificationsAllowed()
        setupNotificationSwitchListener()
    }

    private fun loadValues() {
        binding.switchNotifications.isChecked = prefs.getBoolean(SettingsPrefs.KEY_NOTIFICATIONS, true) && areNotificationsAllowed()
        binding.switchVibration.isChecked = prefs.getBoolean(SettingsPrefs.KEY_VIBRATION, true)
        binding.switchEarlyReminders.isChecked = prefs.getBoolean(SettingsPrefs.KEY_EARLY_REMINDERS, true)
        binding.spinnerThemeMode.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Light", "Dark", "Same as device")
        )
        binding.spinnerThemeMode.setSelection(themeModeToPosition(prefs.getString(SettingsPrefs.KEY_THEME_MODE, SettingsPrefs.THEME_LIGHT)))
        updateSnoozeValueLabels()
    }

    private fun updateSnoozeValueLabels() {
        binding.tvAlarmSnoozeValue.text = formatSnoozeMinutes(SettingsPrefs.getAlarmSnoozeMinutes(requireContext()))
        binding.tvReminderSnoozeValue.text = formatSnoozeMinutes(SettingsPrefs.getReminderSnoozeMinutes(requireContext()))
    }

    private fun formatSnoozeMinutes(minutes: Int): String {
        return getString(R.string.settings_snooze_minutes_value, minutes)
    }

    private fun setupListeners() {


        setupNotificationSwitchListener()

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SettingsPrefs.KEY_VIBRATION, isChecked) }
        }

        binding.switchEarlyReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SettingsPrefs.KEY_EARLY_REMINDERS, isChecked) }
        }

        binding.spinnerThemeMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val themeMode = positionToThemeMode(position)
                if (prefs.getString(SettingsPrefs.KEY_THEME_MODE, SettingsPrefs.THEME_LIGHT) == themeMode) return

                prefs.edit {
                    putString(SettingsPrefs.KEY_THEME_MODE, themeMode)
                        .putBoolean(
                            SettingsPrefs.KEY_DARK_MODE,
                            themeMode == SettingsPrefs.THEME_DARK
                        )
                }
                AppCompatDelegate.setDefaultNightMode(SettingsPrefs.getSavedNightMode(requireContext()))
                requireActivity().recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.rowExactAlarm.setOnClickListener {
            openExactAlarmSettings()
        }

        binding.rowAlarmSnooze.setOnClickListener {
            showSnoozeDurationDialog(
                title = getString(R.string.settings_alarm_snooze_title),
                currentMinutes = SettingsPrefs.getAlarmSnoozeMinutes(requireContext()),
                prefKey = SettingsPrefs.KEY_ALARM_SNOOZE_MINUTES,
                valueView = binding.tvAlarmSnoozeValue
            )
        }

        binding.rowReminderSnooze.setOnClickListener {
            showSnoozeDurationDialog(
                title = getString(R.string.settings_reminder_snooze_title),
                currentMinutes = SettingsPrefs.getReminderSnoozeMinutes(requireContext()),
                prefKey = SettingsPrefs.KEY_REMINDER_SNOOZE_MINUTES,
                valueView = binding.tvReminderSnoozeValue
            )
        }

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun setupNotificationSwitchListener() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SettingsPrefs.KEY_NOTIFICATIONS, isChecked) }
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
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                SettingsPrefs.REQUEST_NOTIFICATIONS
            )
        }
    }

    private fun areNotificationsAllowed(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            })
        } else {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${requireContext().packageName}".toUri()
            })
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${requireContext().packageName}".toUri()
                })
            } else {
                Toast.makeText(requireContext(), "Exact alarm permission is already allowed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Exact alarm permission is already available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSnoozeDurationDialog(
        title: String,
        currentMinutes: Int,
        prefKey: String,
        valueView: TextView
    ) {
        val picker = NumberPicker(requireContext()).apply {
            minValue = SettingsPrefs.MIN_SNOOZE_MINUTES
            maxValue = SettingsPrefs.MAX_SNOOZE_MINUTES
            value = currentMinutes
            wrapSelectorWheel = false
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(picker)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val minutes = picker.value.coerceIn(SettingsPrefs.MIN_SNOOZE_MINUTES, SettingsPrefs.MAX_SNOOZE_MINUTES)
                prefs.edit { putInt(prefKey, minutes) }
                valueView.text = formatSnoozeMinutes(minutes)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_snooze_saved, minutes),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Do you want to log out of Smart Health Reminder?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                requireActivity().finish()
            }
            .show()
    }

    private fun themeModeToPosition(themeMode: String?): Int {
        return when (themeMode) {
            SettingsPrefs.THEME_DARK -> 1
            SettingsPrefs.THEME_SYSTEM -> 2
            else -> 0
        }
    }

    private fun positionToThemeMode(position: Int): String {
        return when (position) {
            1 -> SettingsPrefs.THEME_DARK
            2 -> SettingsPrefs.THEME_SYSTEM
            else -> SettingsPrefs.THEME_LIGHT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
