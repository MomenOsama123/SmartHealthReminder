package com.example.smarthealthreminder.features.ui.viewmodel

import androidx.lifecycle.*
import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.features.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.features.data.local.entity.StepEntity
import com.example.smarthealthreminder.features.model_dashboard.User
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity

class HealthViewModel(val repository: HealthRepository) : ViewModel() {


    // Medication Plans
    val allMedicationPlans: StateFlow<List<MedicationPlanEntity>> = repository.getAllMedicationPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Steps
    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    val todaySteps: StateFlow<StepEntity?> = repository.getStepByDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val lastSevenDaysSteps: StateFlow<List<StepEntity>> = repository.getLastSevenDaysSteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Updates today's steps and automatically calculates calories and distance.
     * Formula used:
     * - Calories: steps * 0.04 (approx. walking burn)
     * - Distance: steps * 0.0008 (approx. km per step)
     */
    fun updateSteps(steps: Int, activeMin: Int) = viewModelScope.launch {
        val calories = (steps * 0.04).toInt()
        val distance = steps * 0.0008
        val target = currentUser.value?.dailyStepGoal ?: 10000

        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(todayDate, steps, target, calories, distance, activeMin))
        } else {
            repository.updateTodayProgress(todayDate, steps, calories, distance, activeMin)
        }
    }
    
    fun resetSteps() = viewModelScope.launch {
        val target = currentUser.value?.dailyStepGoal ?: 10000
        repository.insertOrUpdateStep(StepEntity(todayDate, 0, target, 0, 0.0, 0))
    }

    fun updateSleepQuality(quality: Int) = viewModelScope.launch {
        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(date = todayDate, sleepQuality = quality))
        } else {
            repository.updateSleepQuality(todayDate, quality)
        }
    }

    fun updateFatigueLevel(level: String) = viewModelScope.launch {
        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(date = todayDate, fatigueLevel = level))
        } else {
            repository.updateFatigueLevel(todayDate, level)
        }
    }

    fun updateWaterIntake(ml: Int) = viewModelScope.launch {
        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(date = todayDate, waterIntakeMl = ml))
        } else {
            repository.updateWaterIntake(todayDate, ml)
        }
    }

    fun updateHeartRate(bpm: Int) = viewModelScope.launch {
        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(date = todayDate, heartRateBpm = bpm))
        } else {
            repository.updateHeartRate(todayDate, bpm)
        }
    }

    fun updateDailyGoal(target: Int) = viewModelScope.launch {
        // Update current day's target in steps table
        val current = todaySteps.value
        if (current == null) {
            repository.insertOrUpdateStep(StepEntity(date = todayDate, targetSteps = target))
        } else {
            repository.updateTargetSteps(todayDate, target)
        }

        // Update user profile goal
        currentUser.value?.let { user ->
            val updatedUser = user.copy(dailyStepGoal = target)
            updateCurrentUser(updatedUser)
            // Note: Ideally this should also be saved to Firestore/Local database here
            // ,but we'll stick to the current VM state for now as per project pattern.
        }
    }

    // Alarms
    val allAlarms: StateFlow<List<AlarmEntity>> = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.getRemindersByStatus("Pending")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todayReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .map { reminders ->
            val today = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date())

            reminders.filter { it.date == today }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Counts
    val pendingCount: StateFlow<Int> = repository.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedCount: StateFlow<Int> = repository.getCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val missedCount: StateFlow<Int> = repository.getMissedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Calendar Notes
    val allNoteDates: StateFlow<List<String>> = repository.getAllNoteDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentNote = MutableStateFlow<CalendarNoteEntity?>(null)
    val currentNote: StateFlow<CalendarNoteEntity?> = _currentNote

    fun loadNoteForDate(date: String) = viewModelScope.launch {
        _currentNote.value = repository.getNoteByDate(date)
    }

    fun saveNote(date: String, note: String) = viewModelScope.launch {
        if (note.isBlank()) {
            repository.deleteNote(date)
        } else {
            repository.saveNote(date, note)
        }
        _currentNote.value = CalendarNoteEntity(date, note)
    }

    // Schedule Entries
    val allScheduleEntries: StateFlow<List<ScheduleEntryEntity>> = repository.getAllScheduleEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addScheduleEntry(entry: ScheduleEntryEntity) = viewModelScope.launch { repository.insertScheduleEntry(entry) }
    fun updateScheduleEntry(entry: ScheduleEntryEntity) = viewModelScope.launch { repository.updateScheduleEntry(entry) }
    fun deleteScheduleEntry(entry: ScheduleEntryEntity) = viewModelScope.launch { repository.deleteScheduleEntry(entry) }
    fun deleteScheduleEntryById(id: String) = viewModelScope.launch { repository.deleteScheduleEntryById(id) }

    // Reports
    val allReports: StateFlow<List<ReportEntity>> = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReport(report: ReportEntity) = viewModelScope.launch { repository.insertReport(report) }
    fun updateReport(report: ReportEntity) = viewModelScope.launch { repository.updateReport(report) }
    fun deleteReport(report: ReportEntity) = viewModelScope.launch { repository.deleteReport(report) }
    fun deleteReportById(id: String) = viewModelScope.launch { repository.deleteReportById(id) }

    // Alarm Operations
    fun addAlarm(alarm: AlarmEntity) = viewModelScope.launch { repository.insertAlarm(alarm) }
    fun updateAlarm(alarm: AlarmEntity) = viewModelScope.launch { repository.updateAlarm(alarm) }
    fun deleteAlarm(alarm: AlarmEntity) = viewModelScope.launch { repository.deleteAlarm(alarm) }
    fun deleteAlarmById(id: String) = viewModelScope.launch { repository.deleteAlarmById(id) }
    fun toggleAlarm(id: String, isActive: Boolean) = viewModelScope.launch { repository.toggleAlarmStatus(id, isActive) }
    fun markAlarmCompleted(id: String) = viewModelScope.launch { repository.markAlarmCompleted(id) }
    fun markAlarmSnoozed(alarmId: String, snoozeMinutes: Int) {
        viewModelScope.launch {
            repository.markAlarmSnoozed(alarmId, snoozeMinutes)
        }
    }

    fun resetAlarmToPending(id: String) = viewModelScope.launch { repository.resetAlarmToPending(id) }

    // Reminder Operations
    fun addReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.insertReminder(reminder) }
    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.updateReminder(reminder) }
    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.deleteReminder(reminder) }
    fun deleteReminderById(id: String) = viewModelScope.launch { repository.deleteReminderById(id) }
    fun markReminderDone(id: String) = viewModelScope.launch { repository.markReminderDone(id) }
    fun markReminderMissed(id: String) = viewModelScope.launch { repository.markReminderMissed(id) }
    fun snoozeReminder(id: String, newTime: String) =
        viewModelScope.launch {
            repository.snoozeReminder(id, newTime)


        }
    fun resetReminderStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateReminderStatus(id, status)
        }
    }

    // Profile / User Data
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    fun updateCurrentUser(user: User) {
        _currentUser.value = user
    }
}

class HealthViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}