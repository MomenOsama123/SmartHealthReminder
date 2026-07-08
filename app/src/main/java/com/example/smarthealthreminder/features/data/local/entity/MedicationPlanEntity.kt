package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_plans")
data class MedicationPlanEntity(
    @PrimaryKey
    val id: String,
    val medicineName: String,
    val dosage: String? = null,
    val timesPerDay: Int,
    val timesOfDay: String,
    val startDate: String,       // yyyy-MM-dd
    val durationDays: Int,
    val endDate: String,         // yyyy-MM-dd
    val instructions: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)