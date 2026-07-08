package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import com.example.smarthealthreminder.features.data.local.entity.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity)

    @Query("SELECT * FROM dose_logs WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate DESC")
    fun getLogsForRange(startDate: String, endDate: String): Flow<List<DoseLogEntity>>

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'Taken' AND scheduledDate BETWEEN :startDate AND :endDate")
    suspend fun getTakenCountForRange(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'Missed' AND scheduledDate BETWEEN :startDate AND :endDate")
    suspend fun getMissedCountForRange(startDate: String, endDate: String): Int

    @Query("SELECT * FROM dose_logs WHERE planId = :planId ORDER BY scheduledDate DESC")
    fun getLogsForPlan(planId: String): Flow<List<DoseLogEntity>>
}