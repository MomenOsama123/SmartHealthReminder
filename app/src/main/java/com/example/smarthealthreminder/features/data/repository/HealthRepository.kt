package com.example.smarthealthreminder.features.data.repository

import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.features.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.features.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.data.local.entity.DoseLogEntity

class HealthRepository(private val database: AppDatabase) {

    // Step Operations
    fun getStepByDate(date: String): Flow<StepEntity?> = database.stepDao().getStepByDate(date)
    fun getLastSevenDaysSteps(): Flow<List<StepEntity>> = database.stepDao().getLastSevenDaysSteps()
    suspend fun insertOrUpdateStep(step: StepEntity) = database.stepDao().insertOrUpdateStep(step)
    suspend fun updateTodayProgress(date: String, steps: Int, calories: Int, distance: Double, activeMin: Int) =
        database.stepDao().updateTodayProgress(date, steps, calories, distance, activeMin)

    suspend fun updateSleepQuality(date: String, quality: Int) =
        database.stepDao().updateSleepQuality(date, quality)

    suspend fun updateFatigueLevel(date: String, level: String) =
        database.stepDao().updateFatigueLevel(date, level)

    suspend fun updateWaterIntake(date: String, ml: Int) =
        database.stepDao().updateWaterIntake(date, ml)

    suspend fun updateHeartRate(date: String, bpm: Int) =
        database.stepDao().updateHeartRate(date, bpm)

    suspend fun updateTargetSteps(date: String, target: Int) =
        database.stepDao().updateTargetSteps(date, target)

    // Alarm Operations
    fun getAllAlarms(): Flow<List<AlarmEntity>> = database.alarmDao().getAllAlarms()
    suspend fun getAlarmById(id: String) = database.alarmDao().getAlarmById(id)
    suspend fun insertAlarm(alarm: AlarmEntity) = database.alarmDao().insertAlarm(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = database.alarmDao().updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = database.alarmDao().deleteAlarm(alarm)
    suspend fun deleteAlarmById(id: String) = database.alarmDao().deleteAlarmById(id)
    suspend fun toggleAlarmStatus(id: String, isActive: Boolean) =
        database.alarmDao().updateAlarmStatus(id, isActive)
    suspend fun markAlarmCompleted(id: String) =
        database.alarmDao().updateLastTriggeredStatus(id, "Completed")
    suspend fun markAlarmSnoozed(id: String, snoozeMinutes: Int) =
        database.alarmDao().updateSnoozeStatus(id, "Snoozed", snoozeMinutes)
    suspend fun resetAlarmToPending(id: String) =
        database.alarmDao().updateLastTriggeredStatus(id, "Pending")

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
    suspend fun snoozeReminder(id: String, newTime: String) =
        database.reminderDao().snoozeReminder(id, newTime)

    fun searchReminders(query: String): Flow<List<ReminderEntity>> =
        database.reminderDao().searchReminders("%$query%")

    fun searchAlarms(query: String): Flow<List<AlarmEntity>> =
        database.alarmDao().searchAlarms("%$query%")

    // Counts
    fun getPendingCount(): Flow<Int> = database.reminderDao().getPendingCount()
    fun getCompletedCount(): Flow<Int> = database.reminderDao().getCompletedCount()
    fun getMissedCount(): Flow<Int> = database.reminderDao().getMissedCount()

    // Calendar Notes
    suspend fun getNoteByDate(date: String): CalendarNoteEntity? =
        database.calendarNoteDao().getNoteByDate(date)

    fun getAllNoteDates(): Flow<List<String>> =
        database.calendarNoteDao().getAllNoteDates()

    suspend fun saveNote(date: String, note: String) =
        database.calendarNoteDao().upsertNote(CalendarNoteEntity(date, note))

    suspend fun deleteNote(date: String) =
        database.calendarNoteDao().deleteNoteByDate(date)

    // Schedule Entry Operations
    fun getAllScheduleEntries(): Flow<List<ScheduleEntryEntity>> = database.scheduleEntryDao().getAllEntries()
    fun getScheduleEntriesByDate(date: String): Flow<List<ScheduleEntryEntity>> = database.scheduleEntryDao().getEntriesByDate(date)
    suspend fun insertScheduleEntry(entry: ScheduleEntryEntity) = database.scheduleEntryDao().insertEntry(entry)
    suspend fun updateScheduleEntry(entry: ScheduleEntryEntity) = database.scheduleEntryDao().updateEntry(entry)
    suspend fun deleteScheduleEntry(entry: ScheduleEntryEntity) = database.scheduleEntryDao().deleteEntry(entry)
    suspend fun deleteScheduleEntryById(id: String) = database.scheduleEntryDao().deleteEntryById(id)

    // Report Operations
    fun getAllReports(): Flow<List<ReportEntity>> = database.reportDao().getAllReports()
    fun getReportsByDate(date: String): Flow<List<ReportEntity>> = database.reportDao().getReportsByDate(date)
    suspend fun insertReport(report: ReportEntity) = database.reportDao().insertReport(report)
    suspend fun updateReport(report: ReportEntity) = database.reportDao().updateReport(report)
    suspend fun deleteReport(report: ReportEntity) = database.reportDao().deleteReport(report)
    suspend fun deleteReportById(id: String) = database.reportDao().deleteReportById(id)
    suspend fun updateReminderStatus(id: String, status: String) =
        database.reminderDao().updateReminderStatus(id, status)

    // Medication Plan Operations
    suspend fun insertMedicationPlan(plan: MedicationPlanEntity) =
        database.medicationPlanDao().insertPlan(plan)

    fun getAllMedicationPlans(): Flow<List<MedicationPlanEntity>> =
        database.medicationPlanDao().getAllPlans()

    // Dose Log Operations
    fun getDoseLogsForRange(startDate: String, endDate: String): Flow<List<DoseLogEntity>> =
        database.doseLogDao().getLogsForRange(startDate, endDate)

    suspend fun getTakenCountForRange(startDate: String, endDate: String) =
        database.doseLogDao().getTakenCountForRange(startDate, endDate)

    suspend fun getMissedCountForRange(startDate: String, endDate: String) =
        database.doseLogDao().getMissedCountForRange(startDate, endDate)

    suspend fun getRemindersByPlanId(planId: String) =
        database.reminderDao().getRemindersByPlanId(planId)

    suspend fun deactivateMedicationPlan(planId: String) =
        database.medicationPlanDao().deactivatePlan(planId)
}