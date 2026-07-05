package com.example.smarthealthreminder.features.model_dashboard
data class MedicationHistory(
    val id: Int,
    val doseId: Int,
    val medId: Int,
    val userId: Int,
    val action: String,
    val timestamp: String,
    val details: String?
)