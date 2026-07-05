package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps_table")
data class StepEntity(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val steps: Int = 0,
    val targetSteps: Int = 10000,
    val calories: Int = 0,
    val distanceKm: Double = 0.0,
    val activeMinutes: Int = 0,
    val sleepQuality: Int = 0,
    val fatigueLevel: String = "Low",
    val waterIntakeMl: Int = 0,
    val targetWaterMl: Int = 2000,
    val heartRateBpm: Int = 72
)