package com.example.smarthealthreminder.features.fragment

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.alarm.ReminderScheduler
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import kotlinx.coroutines.launch
import java.util.*

class AddReminderFragment : Fragment() {

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val EARLY_NOTIFICATION_MINUTES = 5
    }

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
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
    private lateinit var rowRecurrence: LinearLayout
    private lateinit var tvRecurrenceValue: TextView

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedCategory: String = "Medicine"
    private var selectedPriority: String = "Medium"
    private var selectedRecurrence: String = RecurrenceHelper.NONE

    private var existingReminderId: String? = null
    private var isEditMode = false
    private var isSaving = false
    private var loadedEntity: ReminderEntity? = null

    private lateinit var repository: HealthRepository
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        repository = HealthRepository(db)
        dbHelper = DatabaseHelper(requireContext())

        existingReminderId = arguments?.getString(EXTRA_REMINDER_ID)
        isEditMode = existingReminderId != null

        initViews(view)
        checkEditMode()
        setupListeners(view)
        updateSaveButtonState()
        
        // Hide bottom nav if we want a full screen feel or handle in MainActivity
    }

    private fun checkEditMode() {
        if (existingReminderId != null) {
            btnSave.text = "Update Reminder"
            btnDelete.visibility = View.VISIBLE
            loadReminderFromDatabase(existingReminderId!!)
        }
    }

    private fun loadReminderFromDatabase(reminderId: String) {
        lifecycleScope.launch {
            val entity = repository.getReminderById(reminderId)
            if (entity == null) {
                Toast.makeText(requireContext(), "Reminder not found", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
                return@launch
            }
            loadedEntity = entity
            populateUi(entity)
        }
    }

    private fun populateUi(entity: ReminderEntity) {
        etTitle.setText(entity.title)
        etDescription.setText(entity.description.orEmpty())
        selectedDate = entity.date.orEmpty()
        selectedTime = entity.time.orEmpty()
        etDate.setText(selectedDate)
        etTime.setText(selectedTime)

        selectedCategory = entity.category ?: "Medicine"
        when (selectedCategory) {
            "Medicine" -> chipGroupCategory.check(R.id.chip_medicine)
            "Appointment" -> chipGroupCategory.check(R.id.chip_appointment)
            "Task" -> chipGroupCategory.check(R.id.chip_task)
            "Custom" -> chipGroupCategory.check(R.id.chip_custom)
        }

        selectedPriority = entity.priority ?: "Medium"
        when (selectedPriority) {
            "Low" -> togglePriority.check(R.id.btn_low)
            "High" -> togglePriority.check(R.id.btn_high)
            else -> togglePriority.check(R.id.btn_medium)
        }

        selectedRecurrence = entity.recurrenceType?.takeIf { RecurrenceHelper.isRecurring(it) }
            ?: if (entity.isRecurring) RecurrenceHelper.DAILY else RecurrenceHelper.NONE
        tvRecurrenceValue.text = selectedRecurrence

        switchEarlyNotification.isChecked = entity.earlyNotification
        switchVibration.isChecked = entity.vibrationEnabled
    }

    private fun initViews(view: View) {
        etTitle = view.findViewById(R.id.et_title)
        etDescription = view.findViewById(R.id.et_description)
        etDate = view.findViewById(R.id.et_date)
        etTime = view.findViewById(R.id.et_time)
        chipGroupCategory = view.findViewById(R.id.chip_group_category)
        togglePriority = view.findViewById(R.id.toggle_priority)
        switchEarlyNotification = view.findViewById(R.id.switch_early_notification)
        switchVibration = view.findViewById(R.id.switch_vibration)
        btnSave = view.findViewById(R.id.btn_save_reminder)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnDelete = view.findViewById(R.id.btn_delete_reminder)
        rowRecurrence = view.findViewById(R.id.row_recurrence)
        tvRecurrenceValue = view.findViewById(R.id.tv_recurrence_value)

        // Hide bottom nav in AddReminderFragment
        view.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE

        val settings = requireContext().getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        switchEarlyNotification.isChecked = settings.getBoolean(SettingsPrefs.KEY_EARLY_REMINDERS, true)
        switchVibration.isChecked = settings.getBoolean(SettingsPrefs.KEY_VIBRATION, true)

        if (!isEditMode) {
            val calendar = Calendar.getInstance()
            selectedDate = String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            etDate.setText(selectedDate)

            selectedTime = String.format(
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
            etTime.setText(selectedTime)
        }
        etDate.showSoftInputOnFocus = false
        etTime.showSoftInputOnFocus = false
    }

    private fun setupListeners(view: View) {
        etDate.setOnClickListener { showDatePicker() }
        etTime.setOnClickListener { showTimePicker() }

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

        btnSave.setOnClickListener { saveReminder() }
        btnCancel.setOnClickListener { navigateToDashboard() }
        btnDelete.setOnClickListener { deleteReminder() }
        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener { navigateToDashboard() }
        rowRecurrence.setOnClickListener { showRecurrencePicker() }
    }

    private fun showRecurrencePicker() {
        val currentIndex = RecurrenceHelper.OPTIONS.indexOf(selectedRecurrence).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Recurrence")
            .setSingleChoiceItems(RecurrenceHelper.OPTIONS, currentIndex) { dialog, which ->
                selectedRecurrence = RecurrenceHelper.OPTIONS[which]
                tvRecurrenceValue.text = selectedRecurrence
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
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
            requireContext(),
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
        if (isSaving) return

        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            return
        }

        val description = etDescription.text.toString().trim()
        val isRecurring = RecurrenceHelper.isRecurring(selectedRecurrence)
        val reminderTimeMillis = if (isRecurring) {
            RecurrenceHelper.computeNextTriggerMillis(selectedDate, selectedTime, selectedRecurrence)
        } else {
            getReminderTimeMillis()
        }

        if (reminderTimeMillis == null) {
            Toast.makeText(requireContext(), "Please choose a valid date and time", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isRecurring && reminderTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Please choose a future time", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), "Allow exact alarms so reminders can ring on time", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            })
            return
        }

        val reminderId = existingReminderId ?: UUID.randomUUID().toString()

        val reminder = ReminderEntity(
            id = reminderId,
            title = title,
            description = description,
            category = selectedCategory,
            date = selectedDate,
            time = selectedTime,
            priority = selectedPriority,
            status = loadedEntity?.status ?: "Pending",
            isRecurring = isRecurring,
            recurrenceType = if (isRecurring) selectedRecurrence else null,
            vibrationEnabled = switchVibration.isChecked,
            earlyNotification = switchEarlyNotification.isChecked,
            earlyNotificationMinutes = if (switchEarlyNotification.isChecked) EARLY_NOTIFICATION_MINUTES else 0
        )

        isSaving = true
        updateSaveButtonState()

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    repository.updateReminder(reminder)
                } else {
                    repository.insertReminder(reminder)
                }

                saveToDatabaseHelper(reminderId, title, description, selectedTime, selectedCategory)

                ReminderScheduler.scheduleReminder(requireContext(), reminder, reminderTimeMillis)

                Toast.makeText(requireContext(), "Reminder saved!", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } finally {
                isSaving = false
                updateSaveButtonState()
            }
        }
    }

    private fun updateSaveButtonState() {
        btnSave.isEnabled = !isSaving
        btnSave.alpha = if (isSaving) 0.6f else 1f
    }

    private fun saveToDatabaseHelper(reminderId: String, title: String, description: String, time: String, category: String) {
        try {
            val sharedPref = requireActivity().getSharedPreferences("HealthSyncPrefs", Context.MODE_PRIVATE)
            val firebaseId = sharedPref.getString("FIREBASE_ID", "") ?: ""
            val user = dbHelper.getUserByFirebaseId(firebaseId)
            val userId = user?.id ?: -1

            if (userId != -1) {
                val values = android.content.ContentValues().apply {
                    put("id", reminderId)
                    put("user_id", userId)
                    put("title", title)
                    put("description", description)
                    put("time", time)
                    put("category", category)
                    put("status", "Pending")
                    put("date", selectedDate)
                    put("priority", selectedPriority)
                    put("is_recurring", if (RecurrenceHelper.isRecurring(selectedRecurrence)) 1 else 0)
                    put("recurrence_type", if (RecurrenceHelper.isRecurring(selectedRecurrence)) selectedRecurrence else null)
                    put("vibration_enabled", if (switchVibration.isChecked) 1 else 0)
                    put("early_notification", if (switchEarlyNotification.isChecked) 1 else 0)
                    put("early_notification_minutes", if (switchEarlyNotification.isChecked) 5 else 0)
                }

                val db = dbHelper.writableDatabase
                db.insert("reminders", null, values)
                db.close()
            }
        } catch (e: Exception) {
            Log.e("ADD_REMINDER", "Failed to save to DatabaseHelper", e)
        }
    }

    private fun navigateToDashboard() {
        (activity as? MainActivity)?.navigateToDestination(MainActivity.DESTINATION_HOME)
    }

    private fun getReminderTimeMillis(): Long? {
        return try {
            val dateParts = selectedDate.split("-")
            val timeParts = selectedTime.split(":")

            if (dateParts.size < 3 || timeParts.size < 2) return null

            Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteReminder() {
        existingReminderId?.let { id ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteReminderById(id)
                        ReminderScheduler.cancelReminderAlarms(requireContext(), id)
                        Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}