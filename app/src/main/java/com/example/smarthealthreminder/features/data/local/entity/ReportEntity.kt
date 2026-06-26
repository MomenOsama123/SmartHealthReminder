package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val date: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
