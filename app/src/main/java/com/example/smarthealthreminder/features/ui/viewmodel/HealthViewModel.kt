package com.example.smarthealthreminder.ui.viewmodel

import androidx.lifecycle.*
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.data.local.entity.ReportEntity
import com.example.smarthealthreminder.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.data.repository.HealthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {

    // Alarms
    val allAlarms: StateFlow<List<AlarmEntity>> = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.getRemindersByStatus("Pending")
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

    // Reminder Operations
    fun addReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.insertReminder(reminder) }
    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.updateReminder(reminder) }
    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.deleteReminder(reminder) }
    fun deleteReminderById(id: String) = viewModelScope.launch { repository.deleteReminderById(id) }
    fun markReminderDone(id: String) = viewModelScope.launch { repository.markReminderDone(id) }
    fun markReminderMissed(id: String) = viewModelScope.launch { repository.markReminderMissed(id) }
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