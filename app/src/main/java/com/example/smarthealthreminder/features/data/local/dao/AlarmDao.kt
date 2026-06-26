package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY time ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: String): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE id = :alarmId AND is_active = 1")
    suspend fun getActiveAlarmById(alarmId: String): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity)

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: String)

    @Query("UPDATE alarms SET is_active = :isActive WHERE id = :alarmId")
    suspend fun updateAlarmStatus(alarmId: String, isActive: Boolean)

    @Query("UPDATE alarms SET last_triggered_status = :status WHERE id = :alarmId")
    suspend fun updateLastTriggeredStatus(alarmId: String, status: String)

    @Query("SELECT COUNT(*) FROM alarms WHERE is_active = 1")
    fun getActiveAlarmCount(): Flow<Int>

    @Query("SELECT * FROM alarms WHERE label LIKE :query ORDER BY time ASC")
    fun searchAlarms(query: String): Flow<List<AlarmEntity>>
}
