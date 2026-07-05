package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import com.example.smarthealthreminder.features.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM steps_table WHERE date = :date")
    fun getStepByDate(date: String): Flow<StepEntity?>

    @Query("SELECT * FROM steps_table ORDER BY date DESC LIMIT 7")
    fun getLastSevenDaysSteps(): Flow<List<StepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStep(step: StepEntity)

    @Query("UPDATE steps_table SET steps = :steps, calories = :calories, distanceKm = :distance, activeMinutes = :activeMin WHERE date = :date")
    suspend fun updateTodayProgress(date: String, steps: Int, calories: Int, distance: Double, activeMin: Int)

    @Query("UPDATE steps_table SET sleepQuality = :quality WHERE date = :date")
    suspend fun updateSleepQuality(date: String, quality: Int)

    @Query("UPDATE steps_table SET fatigueLevel = :level WHERE date = :date")
    suspend fun updateFatigueLevel(date: String, level: String)

    @Query("UPDATE steps_table SET waterIntakeMl = :ml WHERE date = :date")
    suspend fun updateWaterIntake(date: String, ml: Int)

    @Query("UPDATE steps_table SET heartRateBpm = :bpm WHERE date = :date")
    suspend fun updateHeartRate(date: String, bpm: Int)
    @Query("UPDATE steps_table SET targetSteps = :target WHERE date = :date")
    suspend fun updateTargetSteps(date: String, target: Int)
}