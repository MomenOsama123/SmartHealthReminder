package com.example.smarthealthreminder.model_d
data class DailyAdherence(
    val date: String,
    val totalDoses: Int,
    val takenDoses: Int,
    val missedDoses: Int,
    val percentage: Int
)