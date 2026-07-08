package com.example.smarthealthreminder.features.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.model.Alarm
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

class EditAlarmActivity : BaseActivity() {

    companion object {
        const val RESULT_ALARM_SAVED = Activity.RESULT_FIRST_USER + 1
        const val RESULT_ALARM_DELETED = Activity.RESULT_FIRST_USER + 2
        const val EXTRA_ALARM_RESULT = "alarm_result"
        const val EXTRA_ALARM_ID = "alarm_id"

        private const val STATE_ALARM_ID = "state_alarm_id"
        private const val STATE_IS_SAVING = "state_is_saving"
        private const val STATE_PENDING_PERMISSION = "state_pending_permission"
    }

    private lateinit var rootView: androidx.coordinatorlayout.widget.CoordinatorLayout
    private lateinit var etLabel: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var chipGroupDays: com.google.android.material.chip.ChipGroup
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var existingAlarmId: String? = null
    private var isEditMode = false
    private var isSaving = false
    private var pendingSaveAfterPermission = false
    private var loadedEntity: AlarmEntity? = null
    private var pendingDeleteEntity: AlarmEntity? = null

    private lateinit var repository: HealthRepository
    private lateinit var alarmHelper: AlarmHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_alarm_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = AppDatabase.getDatabase(this)
        repository = HealthRepository(db)
        alarmHelper = AlarmHelper(this)

        initViews()

        existingAlarmId = savedInstanceState?.getString(STATE_ALARM_ID)
            ?: intent.getStringExtra(EXTRA_ALARM_ID)
            ?: intent.getStringExtra("alarm_id")

        isSaving = savedInstanceState?.getBoolean(STATE_IS_SAVING, false) ?: false
        pendingSaveAfterPermission = savedInstanceState?.getBoolean(STATE_PENDING_PERMISSION, false) ?: false

        if (existingAlarmId != null) {
            isEditMode = true
            loadAlarmFromDatabase(existingAlarmId!!)
        } else {
            setupNewAlarmDefaults()
        }

        setupListeners()
        updateSaveButtonState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ALARM_ID, existingAlarmId)
        outState.putBoolean(STATE_IS_SAVING, isSaving)
        outState.putBoolean(STATE_PENDING_PERMISSION, pendingSaveAfterPermission)
    }

    override fun onResume() {
        super.onResume()
        if (pendingSaveAfterPermission && alarmHelper.canScheduleExactAlarm()) {
            pendingSaveAfterPermission = false
            saveAlarm()
        }
    }

    private fun initViews() {
        rootView = findViewById(R.id.edit_alarm_root)
        etLabel = findViewById(R.id.et_label)
        timePicker = findViewById(R.id.time_picker)
        chipGroupDays = findViewById(R.id.chip_group_days)
        btnSave = findViewById(R.id.btn_save)
        btnDelete = findViewById(R.id.btn_delete)
        timePicker.setIs24HourView(false)
    }

    private fun setupNewAlarmDefaults() {
        isEditMode = false
        btnSave.text = getString(R.string.save_alarm)
        btnDelete.visibility = android.view.View.GONE
        etLabel.setText("")
        val calendar = Calendar.getInstance()
        timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = calendar.get(Calendar.MINUTE)
    }

    private fun loadAlarmFromDatabase(alarmId: String) {
        lifecycleScope.launch {
            val entity = repository.getAlarmById(alarmId)
            if (entity == null) {
                Toast.makeText(this@EditAlarmActivity, "Alarm not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            loadedEntity = entity
            populateUi(entity)
        }
    }

    private fun populateUi(entity: AlarmEntity) {
        btnSave.text = "Update Alarm"
        btnDelete.visibility = android.view.View.VISIBLE
        etLabel.setText(entity.label)

        val (hour24, minute) = AlarmHelper.parseAlarmTime(entity.time, entity.amPm)
        timePicker.hour = hour24
        timePicker.minute = minute

        clearDaySelection()
        selectDays(entity.repeatDays.orEmpty())
    }

    private fun clearDaySelection() {
        listOf(
            R.id.chip_sun, R.id.chip_mon, R.id.chip_tue, R.id.chip_wed,
            R.id.chip_thu, R.id.chip_fri, R.id.chip_sat
        ).forEach { chipId ->
            findViewById<com.google.android.material.chip.Chip>(chipId)?.isChecked = false
        }
    }

    private fun selectDays(days: String) {
        val dayMap = mapOf(
            Calendar.SUNDAY to R.id.chip_sun,
            Calendar.MONDAY to R.id.chip_mon,
            Calendar.TUESDAY to R.id.chip_tue,
            Calendar.WEDNESDAY to R.id.chip_wed,
            Calendar.THURSDAY to R.id.chip_thu,
            Calendar.FRIDAY to R.id.chip_fri,
            Calendar.SATURDAY to R.id.chip_sat
        )
        AlarmHelper.parseRepeatDays(days).forEach { dayOfWeek ->
            dayMap[dayOfWeek]?.let { chipId ->
                findViewById<com.google.android.material.chip.Chip>(chipId)?.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener { saveAlarm() }
        btnDelete.setOnClickListener { confirmDeleteAlarm() }
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun validateInput(): Boolean {
        val label = etLabel.text.toString().trim()
        if (label.isEmpty()) {
            etLabel.error = "Please enter alarm name"
            etLabel.requestFocus()
            return false
        }

        val hour24 = timePicker.hour
        val minute = timePicker.minute
        if (!AlarmHelper.isValidTime(hour24, minute)) {
            Toast.makeText(this, "Invalid time selected", Toast.LENGTH_SHORT).show()
            return false
        }

        val selectedDays = getSelectedDays()
        if (selectedDays.isNotEmpty()) {
            val parsedDays = AlarmHelper.parseRepeatDays(selectedDays)
            val selectedCount = chipGroupDays.checkedChipIds.size
            if (parsedDays.isEmpty() || parsedDays.size != selectedCount) {
                Toast.makeText(this, "Invalid repeat day selection", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun saveAlarm() {
        if (isSaving) return
        if (!validateInput()) return

        if (!alarmHelper.canScheduleExactAlarm()) {
            requestExactAlarmPermission()
            return
        }

        isSaving = true
        updateSaveButtonState()

        val label = etLabel.text.toString().trim()
        val hour24 = timePicker.hour
        val minute = timePicker.minute
        val amPm = if (hour24 < 12) "AM" else "PM"
        val displayHour = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val timeString = String.format(Locale.US, "%02d:%02d", displayHour, minute)
        val selectedDays = getSelectedDays()

        val alarm = AlarmEntity(
            id = existingAlarmId ?: UUID.randomUUID().toString(),
            label = label,
            time = timeString,
            amPm = amPm,
            category = loadedEntity?.category ?: "MEDICINE",
            isActive = loadedEntity?.isActive ?: true,
            repeatDays = selectedDays.ifBlank { null },
            sound = loadedEntity?.sound,
            vibrationEnabled = loadedEntity?.vibrationEnabled ?: false,
            gradualVolume = loadedEntity?.gradualVolume ?: false,
            autoSnoozeMinutes = loadedEntity?.autoSnoozeMinutes ?: 0,
            cognitiveLockEnabled = loadedEntity?.cognitiveLockEnabled ?: false,
            lastTriggeredStatus = loadedEntity?.lastTriggeredStatus ?: "Pending"
        )

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    repository.updateAlarm(alarm)
                } else {
                    repository.insertAlarm(alarm)
                }

                val alarmModel = toAlarmModel(alarm)
                var scheduled = false
                if (alarm.isActive) {
                    scheduled = alarmHelper.scheduleAlarm(alarmModel)
                    if (!scheduled) {
                        repository.updateAlarm(alarm.copy(isActive = false))
                        Toast.makeText(
                            this@EditAlarmActivity,
                            "Alarm saved but deactivated — allow exact alarms to enable it",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    alarmHelper.cancelAlarm(alarm.id)
                    scheduled = true
                }

                if (scheduled && alarm.isActive) {
                    Toast.makeText(
                        this@EditAlarmActivity,
                        "Alarm ${if (isEditMode) "updated" else "saved"}: $label",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                setResult(RESULT_ALARM_SAVED, Intent().apply {
                    putExtra(EXTRA_ALARM_RESULT, alarm.id)
                })
                finish()
            } finally {
                isSaving = false
                updateSaveButtonState()
            }
        }
    }

    private fun requestExactAlarmPermission() {
        MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
            .setTitle("Exact alarm permission required")
            .setMessage(
                "Smart Health Reminder needs permission to schedule exact alarms " +
                        "so your alarm rings at the correct time. You'll be taken to Settings to allow this."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                pendingSaveAfterPermission = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAlarm() {
        MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Delete") { _, _ -> deleteAlarmWithUndo() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAlarmWithUndo() {
        val id = existingAlarmId ?: return
        if (isSaving) return

        lifecycleScope.launch {
            val entity = repository.getAlarmById(id) ?: run {
                Toast.makeText(this@EditAlarmActivity, "Alarm not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            alarmHelper.cancelAllPendingIntents(id)
            repository.deleteAlarmById(id)
            pendingDeleteEntity = entity
            btnDelete.isEnabled = false
            btnSave.isEnabled = false

            Snackbar.make(rootView, "Alarm deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    restoreDeletedAlarm()
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                        if (event != DISMISS_EVENT_ACTION && pendingDeleteEntity != null) {
                            setResult(RESULT_ALARM_DELETED, Intent().apply {
                                putExtra("deleted_alarm_id", id)
                            })
                            finish()
                        }
                    }
                })
                .show()
        }
    }

    private fun restoreDeletedAlarm() {
        val entity = pendingDeleteEntity ?: return
        lifecycleScope.launch {
            repository.insertAlarm(entity)
            if (entity.isActive) {
                alarmHelper.scheduleAlarm(toAlarmModel(entity))
            }
            pendingDeleteEntity = null
            btnDelete.isEnabled = true
            btnSave.isEnabled = true
            Toast.makeText(this@EditAlarmActivity, "Alarm restored", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedDays(): String {
        val chipIds = mapOf(
            R.id.chip_sun to Calendar.SUNDAY,
            R.id.chip_mon to Calendar.MONDAY,
            R.id.chip_tue to Calendar.TUESDAY,
            R.id.chip_wed to Calendar.WEDNESDAY,
            R.id.chip_thu to Calendar.THURSDAY,
            R.id.chip_fri to Calendar.FRIDAY,
            R.id.chip_sat to Calendar.SATURDAY
        )

        val days = mutableListOf<Int>()
        chipIds.forEach { (id, day) ->
            if (findViewById<com.google.android.material.chip.Chip>(id)?.isChecked == true) {
                days.add(day)
            }
        }
        return AlarmHelper.formatRepeatDays(days)
    }

    private fun toAlarmModel(entity: AlarmEntity): Alarm {
        return Alarm(
            id = entity.id,
            label = entity.label,
            time = entity.time,
            amPm = entity.amPm,
            category = entity.category,
            isActive = entity.isActive,
            repeatDays = entity.repeatDays,
            sound = entity.sound,
            vibrationEnabled = entity.vibrationEnabled,
            gradualVolume = entity.gradualVolume,
            autoSnoozeMinutes = entity.autoSnoozeMinutes,
            cognitiveLockEnabled = entity.cognitiveLockEnabled
        )
    }

    private fun updateSaveButtonState() {
        btnSave.isEnabled = !isSaving
        btnSave.alpha = if (isSaving) 0.6f else 1f
    }
}
