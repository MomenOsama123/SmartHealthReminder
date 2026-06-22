package com.example.smarthealthreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_entries")
data class ScheduleEntryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    val category: String? = "General",
    val createdAt: Long = System.currentTimeMillis()
)
