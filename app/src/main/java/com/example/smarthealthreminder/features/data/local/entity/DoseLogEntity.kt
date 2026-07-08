package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val reminderId: String,
    val scheduledDate: String,   // yyyy-MM-dd —
    val scheduledTime: String,   // HH:mm
    val status: String,          // "Taken" or "Missed"
    val actionTimestamp: Long = System.currentTimeMillis()
)