package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,

    // الحقول الجديدة اللي ضفناها عشان الشاشة:
    val adherencePercentage: Int,
    val missedDoses: Int,
    val symptomsOverview: String,
    val aiInsight1: String,
    val aiInsight2: String,

    val date: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)