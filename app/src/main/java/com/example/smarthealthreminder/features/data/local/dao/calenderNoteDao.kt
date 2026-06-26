package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import com.example.smarthealthreminder.features.data.local.entity.CalendarNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarNoteDao {
    @Query("SELECT * FROM calendar_notes WHERE date = :date LIMIT 1")
    suspend fun getNoteByDate(date: String): CalendarNoteEntity?

    @Query("SELECT date FROM calendar_notes WHERE note != ''")
    fun getAllNoteDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: CalendarNoteEntity)

    @Query("DELETE FROM calendar_notes WHERE date = :date")
    suspend fun deleteNoteByDate(date: String)
}