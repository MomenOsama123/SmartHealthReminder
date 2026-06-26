package com.example.smarthealthreminder.features.model_d
data class MedicationStats(
    val totalMeds: Int,
    val takenToday: Int,
    val missedToday: Int,
    val adherencePercentage: Int
)