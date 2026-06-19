package com.example.smarthealthreminder.features.activity

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.*

class AddReminderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REMINDER_RESULT = "reminder_result"
        private const val EARLY_NOTIFICATION_MINUTES = 5
        private const val EARLY_NOTIFICATION_REQUEST_OFFSET = 10_000
    }

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var chipGroupCategory: com.google.android.material.chip.ChipGroup
    private lateinit var togglePriority: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var switchEarlyNotification: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchVibration: androidx.appcompat.widget.SwitchCompat
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnCancel: TextView
    private lateinit var btnDelete: TextView

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedCategory: String = "Medicine"
    private var selectedPriority: String = "Medium"

    private var existingReminderId: String? = null
    private var isEditMode = false

    private lateinit var repository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_add_reminder)

        val db = AppDatabase.getDatabase(this)
        repository = HealthRepository(db)

        initViews()
        checkEditMode()
        setupListeners()
        BottomNavHelper.setup(this, findViewById<BottomNavigationView>(R.id.bottom_navigation))
    }

    private fun checkEditMode() {
        existingReminderId = intent.getStringExtra("reminder_id")
        if (existingReminderId != null) {
            isEditMode = true
            btnSave.text = "Update Reminder"
            btnDelete.visibility = View.VISIBLE
            
            etTitle.setText(intent.getStringExtra("reminder_title"))
            etDescription.setText(intent.getStringExtra("reminder_desc"))
            selectedDate = intent.getStringExtra("reminder_date") ?: ""
            selectedTime = intent.getStringExtra("reminder_time") ?: ""
            etDate.setText(selectedDate)
            etTime.setText(selectedTime)
            
            selectedCategory = intent.getStringExtra("reminder_category") ?: "Medicine"
            // Select category chip
            when (selectedCategory) {
                "Medicine" -> chipGroupCategory.check(R.id.chip_medicine)
                "Appointment" -> chipGroupCategory.check(R.id.chip_appointment)
                "Task" -> chipGroupCategory.check(R.id.chip_task)
                "Custom" -> chipGroupCategory.check(R.id.chip_custom)
            }
        }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etDescription = findViewById(R.id.et_description)
        etDate = findViewById(R.id.et_date)
        etTime = findViewById(R.id.et_time)
        chipGroupCategory = findViewById(R.id.chip_group_category)
        togglePriority = findViewById(R.id.toggle_priority)
        switchEarlyNotification = findViewById(R.id.switch_early_notification)
        switchVibration = findViewById(R.id.switch_vibration)
        btnSave = findViewById(R.id.btn_save_reminder)
        btnCancel = findViewById(R.id.btn_cancel)
        btnDelete = findViewById(R.id.btn_delete_reminder)

        val settings = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        switchEarlyNotification.isChecked = settings.getBoolean(SettingsActivity.KEY_EARLY_REMINDERS, true)
        switchVibration.isChecked = settings.getBoolean(SettingsActivity.KEY_VIBRATION, true)

        val calendar = Calendar.getInstance()
        selectedDate = String.format(
            "%02d/%02d/%04d",
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.YEAR)
        )
        etDate.setText(selectedDate)

        selectedTime = String.format(
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
        etTime.setText(selectedTime)
    }

    private fun setupListeners() {
        etDate.setOnClickListener {
            showDatePicker()
        }

        etTime.setOnClickListener {
            showTimePicker()
        }

        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_medicine -> selectedCategory = "Medicine"
                R.id.chip_appointment -> selectedCategory = "Appointment"
                R.id.chip_task -> selectedCategory = "Task"
                R.id.chip_custom -> selectedCategory = "Custom"
            }
        }

        togglePriority.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedPriority = when (checkedId) {
                    R.id.btn_low -> "Low"
                    R.id.btn_medium -> "Medium"
                    R.id.btn_high -> "High"
                    else -> "Medium"
                }
            }
        }

        btnSave.setOnClickListener {
            saveReminder()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // Delete button
        btnDelete.setOnClickListener {
            deleteReminder()
        }

        // Back button
        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%02d/%02d/%04d", month + 1, day, year)
                etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                etTime.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveReminder() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            return
        }

        val description = etDescription.text.toString().trim()
        val reminderTimeMillis = getReminderTimeMillis()
        if (reminderTimeMillis == null) {
            Toast.makeText(this, "Please choose a valid date and time", Toast.LENGTH_SHORT).show()
            return
        }

        if (reminderTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please choose a future time", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Allow exact alarms so reminders can ring on time", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
            return
        }

        val reminder = ReminderEntity(
            id = existingReminderId ?: UUID.randomUUID().toString(),
            title = title,
            description = description,
            category = selectedCategory,
            date = selectedDate,
            time = selectedTime,
            priority = selectedPriority,
            status = "Pending",
            vibrationEnabled = switchVibration.isChecked,
            earlyNotification = switchEarlyNotification.isChecked,
            earlyNotificationMinutes = if (switchEarlyNotification.isChecked) EARLY_NOTIFICATION_MINUTES else 0
        )

        lifecycleScope.launch {
            if (isEditMode) {
                repository.updateReminder(reminder)
            } else {
                repository.insertReminder(reminder)
            }

            scheduleReminderNotification(reminder, reminderTimeMillis)

            Toast.makeText(this@AddReminderActivity, "Reminder saved!", Toast.LENGTH_SHORT).show()

            val resultIntent = Intent().apply {
                putExtra(EXTRA_REMINDER_RESULT, reminder.id)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getReminderTimeMillis(): Long? {
        return try {
            val dateParts = selectedDate.split("/")
            val timeParts = selectedTime.split(":")

            if (dateParts.size < 3 || timeParts.size < 2) return null

            Calendar.getInstance().apply {
                set(Calendar.MONTH, dateParts[0].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[1].toInt())
                set(Calendar.YEAR, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun scheduleReminderNotification(reminder: ReminderEntity, reminderTimeMillis: Long) {
        scheduleReminderAlarm(
            reminder = reminder,
            triggerAtMillis = reminderTimeMillis,
            requestCode = reminder.id.hashCode(),
            title = reminder.title,
            description = reminder.description ?: "Time for your health reminder!"
        )

        if (reminder.earlyNotification) {
            val earlyTimeMillis = reminderTimeMillis - EARLY_NOTIFICATION_MINUTES * 60 * 1000L
            if (earlyTimeMillis > System.currentTimeMillis()) {
                scheduleReminderAlarm(
                    reminder = reminder,
                    triggerAtMillis = earlyTimeMillis,
                    requestCode = reminder.id.hashCode() + EARLY_NOTIFICATION_REQUEST_OFFSET,
                    title = reminder.title,
                    description = "Upcoming in $EARLY_NOTIFICATION_MINUTES minutes"
                )
            }
        }
    }

    private fun scheduleReminderAlarm(
        reminder: ReminderEntity,
        triggerAtMillis: Long,
        requestCode: Int,
        title: String,
        description: String
    ) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", title)
            putExtra("reminder_description", description)
            putExtra("reminder_time", reminder.time)
            putExtra("vibration_enabled", reminder.vibrationEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun deleteReminder() {
        existingReminderId?.let { id ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteReminderById(id)
                        cancelReminderNotification(id)
                        Toast.makeText(this@AddReminderActivity, "Reminder deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun cancelReminderNotification(reminderId: String) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
