package com.example.smarthealthreminder.ui.viewmodel

import androidx.lifecycle.*
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
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

    // Alarm Operations
    fun addAlarm(alarm: AlarmEntity) = viewModelScope.launch {
        repository.insertAlarm(alarm)
    }

    fun updateAlarm(alarm: AlarmEntity) = viewModelScope.launch {
        repository.updateAlarm(alarm)
    }

    fun deleteAlarm(alarm: AlarmEntity) = viewModelScope.launch {
        repository.deleteAlarm(alarm)
    }

    fun deleteAlarmById(id: String) = viewModelScope.launch {
        repository.deleteAlarmById(id)
    }

    fun toggleAlarm(id: String, isActive: Boolean) = viewModelScope.launch {
        repository.toggleAlarmStatus(id, isActive)
    }

    // Reminder Operations
    fun addReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.insertReminder(reminder)
    }

    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder)
    }

    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.deleteReminder(reminder)
    }

    fun deleteReminderById(id: String) = viewModelScope.launch {
        repository.deleteReminderById(id)
    }

    fun markReminderDone(id: String) = viewModelScope.launch {
        repository.markReminderDone(id)
    }

    fun markReminderMissed(id: String) = viewModelScope.launch {
        repository.markReminderMissed(id)
    }
}

// Factory
class HealthViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
