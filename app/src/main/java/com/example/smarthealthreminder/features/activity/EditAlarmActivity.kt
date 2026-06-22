package com.example.smarthealthreminder.features.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.model.Alarm
import kotlinx.coroutines.launch
import java.util.*

class EditAlarmActivity : AppCompatActivity() {

    companion object {
        const val RESULT_ALARM_SAVED = Activity.RESULT_FIRST_USER + 1
        const val RESULT_ALARM_DELETED = Activity.RESULT_FIRST_USER + 2
        const val EXTRA_ALARM_RESULT = "alarm_result"
    }

    private lateinit var etLabel: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var chipGroupDays: com.google.android.material.chip.ChipGroup
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var existingAlarmId: String? = null
    private var isEditMode = false

    private lateinit var repository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        val db = AppDatabase.getDatabase(this)
        repository = HealthRepository(db)

        initViews()
        checkEditMode()
        setupListeners()
    }

    private fun initViews() {
        etLabel = findViewById(R.id.et_label)
        timePicker = findViewById(R.id.time_picker)
        chipGroupDays = findViewById(R.id.chip_group_days)
        btnSave = findViewById(R.id.btn_save)
        btnDelete = findViewById(R.id.btn_delete)

        timePicker.setIs24HourView(false)
    }

    private fun checkEditMode() {
        existingAlarmId = intent.getStringExtra("alarm_id")

        if (existingAlarmId != null) {
            isEditMode = true
            btnSave.text = "Update Alarm"
            btnDelete.visibility = android.view.View.VISIBLE

            // Fill existing data dynamically from intent
            etLabel.setText(intent.getStringExtra("alarm_label") ?: "")
            val time = intent.getStringExtra("alarm_time") ?: "08:00"
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                // Convert to 24h for TimePicker
                val isPm = intent.getStringExtra("alarm_am_pm") == "PM"
                val hour24 = when {
                    hour == 12 && !isPm -> 0
                    hour == 12 && isPm -> 12
                    isPm -> hour + 12
                    else -> hour
                }
                timePicker.hour = hour24
                timePicker.minute = minute
            }

            // Select repeat days
            val repeatDays = intent.getStringExtra("alarm_repeat_days") ?: ""
            selectDays(repeatDays)
        } else {
            isEditMode = false
            btnSave.text = "Save Alarm"
            btnDelete.visibility = android.view.View.GONE
            // Time picker starts at current time
            etLabel.setText("")
            val calendar = Calendar.getInstance()
            timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = calendar.get(Calendar.MINUTE)
        }
    }

    private fun selectDays(days: String) {
        val dayMap = mapOf(
            "Sun" to R.id.chip_sun,
            "Mon" to R.id.chip_mon,
            "Tue" to R.id.chip_tue,
            "Wed" to R.id.chip_wed,
            "Thu" to R.id.chip_thu,
            "Fri" to R.id.chip_fri,
            "Sat" to R.id.chip_sat,
            "M" to R.id.chip_mon,
            "W" to R.id.chip_wed,
            "F" to R.id.chip_fri
        )
        days.split(" ").forEach { day ->
            dayMap[day]?.let { chipId ->
                val chip = findViewById<com.google.android.material.chip.Chip>(chipId)
                chip?.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveAlarm()
        }

        btnDelete.setOnClickListener {
            deleteAlarm()
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun saveAlarm() {
        val label = etLabel.text.toString().trim()
        if (label.isEmpty()) {
            etLabel.error = "Please enter alarm name"
            return
        }

        val hour24 = timePicker.hour
        val minute = timePicker.minute
        val amPm = if (hour24 < 12) "AM" else "PM"
        val displayHour = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val timeString = String.format("%02d:%02d", displayHour, minute)
        val time24String = String.format("%02d:%02d", hour24, minute)

        val selectedDays = getSelectedDays()
        val alarmHelper = com.example.smarthealthreminder.alarm.AlarmHelper(this@EditAlarmActivity)

        if (!alarmHelper.canScheduleExactAlarm()) {
            Toast.makeText(
                this,
                "Allow exact alarms so this alarm can ring on time",
                Toast.LENGTH_LONG
            ).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            return
        }

        val alarm = AlarmEntity(
            id = existingAlarmId ?: UUID.randomUUID().toString(),
            label = label,
            time = timeString,
            amPm = amPm,
            category = "MEDICINE",
            isActive = true,
            repeatDays = selectedDays
        )

        lifecycleScope.launch {
            if (isEditMode) {
                repository.updateAlarm(alarm)
            } else {
                repository.insertAlarm(alarm)
            }

            val alarmModel = com.example.smarthealthreminder.features.model.Alarm(
                id = alarm.id,
                label = alarm.label,
                time = time24String,
                amPm = alarm.amPm,
                category = alarm.category,
                isActive = alarm.isActive,
                repeatDays = selectedDays
            )

            if (alarm.isActive) {
                val scheduled = alarmHelper.scheduleAlarm(alarmModel)
                if (!scheduled) {
                    Toast.makeText(
                        this@EditAlarmActivity,
                        "Allow exact alarms so this alarm can ring on time",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            Toast.makeText(
                this@EditAlarmActivity,
                "Alarm ${if (isEditMode) "updated" else "saved"}: $label",
                Toast.LENGTH_SHORT
            ).show()

            val resultIntent = Intent().apply {
                putExtra(EXTRA_ALARM_RESULT, alarm.id)
            }
            setResult(RESULT_ALARM_SAVED, resultIntent)
            finish()
        }
    }

    private fun getSelectedDays(): String {
        val days = StringBuilder()
        val chipIds = mapOf(
            R.id.chip_sun to "Sun",
            R.id.chip_mon to "Mon",
            R.id.chip_tue to "Tue",
            R.id.chip_wed to "Wed",
            R.id.chip_thu to "Thu",
            R.id.chip_fri to "Fri",
            R.id.chip_sat to "Sat"
        )

        chipIds.forEach { (id, day) ->
            val chip = findViewById<com.google.android.material.chip.Chip>(id)
            if (chip?.isChecked == true) {
                days.append("$day ")
            }
        }

        return days.toString().trim()
    }

    private fun deleteAlarm() {
        lifecycleScope.launch {
            existingAlarmId?.let { id ->
                val alarmModel = com.example.smarthealthreminder.features.model.Alarm(
                    id = id,
                    label = "",
                    time = "",
                    amPm = "",
                    category = ""
                )
                val alarmHelper = com.example.smarthealthreminder.alarm.AlarmHelper(this@EditAlarmActivity)
                alarmHelper.cancelAlarm(alarmModel)

                repository.deleteAlarmById(id)

                val resultIntent = Intent().apply {
                    putExtra("deleted_alarm_id", id)
                }
                setResult(RESULT_ALARM_DELETED, resultIntent)

                Toast.makeText(this@EditAlarmActivity, "Alarm deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
