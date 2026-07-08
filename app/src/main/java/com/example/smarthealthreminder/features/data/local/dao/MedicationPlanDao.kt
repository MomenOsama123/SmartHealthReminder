package com.example.smarthealthreminder.features.data.local.dao

import androidx.room.*
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MedicationPlanEntity)

    @Query("SELECT * FROM medication_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<MedicationPlanEntity>>

    @Query("SELECT * FROM medication_plans WHERE id = :planId")
    suspend fun getPlanById(planId: String): MedicationPlanEntity?

    @Query("UPDATE medication_plans SET isActive = 0 WHERE id = :planId")
    suspend fun deactivatePlan(planId: String)
}