package com.example.smarthealthreminder.features.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.data.repository.HealthRepository
import kotlinx.coroutines.launch
import java.util.*
import android.content.Intent



class AddReminderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REMINDER_RESULT = "reminder_result"
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

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedCategory: String = "Medicine"
    private var selectedPriority: String = "Medium"

    private lateinit var repository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_add_reminder)

        // Init repository
        val db = AppDatabase.getDatabase(this)
        repository = HealthRepository(db)

        initViews()
        setupListeners()
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

        // Set default date to today
        val calendar = Calendar.getInstance()
        selectedDate = String.format("%02d/%02d/%04d", 
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.YEAR))
        etDate.setText(selectedDate)

        // Set default time
        selectedTime = String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE))
        etTime.setText(selectedTime)
    }

    private fun setupListeners() {
        // Date picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        // Time picker
        etTime.setOnClickListener {
            showTimePicker()
        }

        // Category selection
        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_medicine -> selectedCategory = "Medicine"
                R.id.chip_appointment -> selectedCategory = "Appointment"
                R.id.chip_task -> selectedCategory = "Task"
                R.id.chip_custom -> selectedCategory = "Custom"
            }
        }

        // Priority selection
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

        // Save button
        btnSave.setOnClickListener {
            saveReminder()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Back button
        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate = String.format("%02d/%02d/%04d", month + 1, day, year)
            etDate.setText(selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 
           calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            selectedTime = String.format("%02d:%02d", hour, minute)
            etTime.setText(selectedTime)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun saveReminder() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            return
        }

        val description = etDescription.text.toString().trim()

        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            category = selectedCategory,
            date = selectedDate,
            time = selectedTime,
            priority = selectedPriority,
            status = "Pending",
            vibrationEnabled = switchVibration.isChecked,
            earlyNotification = switchEarlyNotification.isChecked
        )

        lifecycleScope.launch {
            repository.insertReminder(reminder)
            Toast.makeText(this@AddReminderActivity, 
                "Reminder saved!", Toast.LENGTH_SHORT).show()

            // Return result
            val resultIntent = android.content.Intent().apply {
                putExtra(EXTRA_REMINDER_RESULT, reminder.id)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
