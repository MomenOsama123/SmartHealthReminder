package com.example.smarthealthreminder.model_d
data class MedicationStats(
    val totalMeds: Int,
    val takenToday: Int,
    val missedToday: Int,
    val adherencePercentage: Int
)