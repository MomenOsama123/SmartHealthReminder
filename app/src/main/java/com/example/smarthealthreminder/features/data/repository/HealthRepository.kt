package com.example.smarthealthreminder.data.repository

import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val database: AppDatabase) {

    // Alarm Operations
    fun getAllAlarms(): Flow<List<AlarmEntity>> = database.alarmDao().getAllAlarms()
    suspend fun getAlarmById(id: String) = database.alarmDao().getAlarmById(id)
    suspend fun insertAlarm(alarm: AlarmEntity) = database.alarmDao().insertAlarm(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = database.alarmDao().updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = database.alarmDao().deleteAlarm(alarm)
    suspend fun deleteAlarmById(id: String) = database.alarmDao().deleteAlarmById(id)
    suspend fun toggleAlarmStatus(id: String, isActive: Boolean) = 
        database.alarmDao().updateAlarmStatus(id, isActive)

    // Reminder Operations
    fun getAllReminders(): Flow<List<ReminderEntity>> = database.reminderDao().getAllReminders()
    fun getRemindersByStatus(status: String): Flow<List<ReminderEntity>> = 
        database.reminderDao().getRemindersByStatus(status)
    suspend fun getReminderById(id: String) = database.reminderDao().getReminderById(id)
    suspend fun insertReminder(reminder: ReminderEntity) = database.reminderDao().insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = database.reminderDao().updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = database.reminderDao().deleteReminder(reminder)
    suspend fun deleteReminderById(id: String) = database.reminderDao().deleteReminderById(id)
    suspend fun markReminderDone(id: String) = database.reminderDao().updateReminderStatus(id, "Completed")
    suspend fun markReminderMissed(id: String) = database.reminderDao().updateReminderStatus(id, "Missed")

    fun searchReminders(query: String): Flow<List<ReminderEntity>> = 
        database.reminderDao().searchReminders("%$query%")

    fun searchAlarms(query: String): Flow<List<AlarmEntity>> = 
        database.alarmDao().searchAlarms("%$query%")

    // Counts
    fun getPendingCount(): Flow<Int> = database.reminderDao().getPendingCount()
    fun getCompletedCount(): Flow<Int> = database.reminderDao().getCompletedCount()
    fun getMissedCount(): Flow<Int> = database.reminderDao().getMissedCount()
}
