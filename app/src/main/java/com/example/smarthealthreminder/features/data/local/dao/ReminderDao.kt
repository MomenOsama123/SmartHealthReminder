package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity

@Dao
interface ReminderDao {

    ///yehia added this:
    // دالة بتجيب عدد الـ Completed والـ Missed في وقت واحد عشان الحسبة
    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'Completed'")
    suspend fun getCompletedCountRaw(): Int

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'Missed'")
    suspend fun getMissedCountRaw(): Int
    @Query("SELECT * FROM reminders ORDER BY date DESC, time DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = :status")
    fun getRemindersByStatus(status: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE date = :date")
    fun getRemindersByDate(date: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: String)

    @Query("UPDATE reminders SET status = :status WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: String, status: String)

    @Query("SELECT * FROM reminders WHERE title LIKE :query OR description LIKE :query ORDER BY date DESC")
    fun searchReminders(query: String): Flow<List<ReminderEntity>>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'Pending'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'Completed'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'Missed'")
    fun getMissedCount(): Flow<Int>
    @Query("""
    UPDATE reminders
    SET time = :newTime,
        status = 'Snoozed'
    WHERE id = :reminderId
""")
    suspend fun snoozeReminder(
        reminderId: String,
        newTime: String
    )
}
