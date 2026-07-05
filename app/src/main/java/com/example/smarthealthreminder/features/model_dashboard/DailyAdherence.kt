package com.example.smarthealthreminder.features.model_dashboard
data class DailyAdherence(
    val date: String,
    val totalDoses: Int,
    val takenDoses: Int,
    val missedDoses: Int,
    val percentage: Int
)