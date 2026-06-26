package com.example.smarthealthreminder.features.model_d

data class Medication(
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val dosage: String? = null,
    val timeOfDay: String,
    val instructions: String? = null,
    val iconType: String = "pill",
    val isActive: Boolean = true,
    val startDate: String? = null,
    val endDate: String? = null,
    val notes: String? = null,
    val createdAt: String? = null
)