package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEntryDao {
    @Query("SELECT * FROM schedule_entries ORDER BY date DESC, time DESC")
    fun getAllEntries(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE date = :date")
    fun getEntriesByDate(date: String): Flow<List<ScheduleEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ScheduleEntryEntity)

    @Update
    suspend fun updateEntry(entry: ScheduleEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: ScheduleEntryEntity)

    @Query("DELETE FROM schedule_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)
}
