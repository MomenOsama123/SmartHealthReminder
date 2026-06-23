package com.example.smarthealthreminder.model_d
enum class DoseStatus {
    PENDING, TAKEN, MISSED, SNOOZED
}

data class DailyDose(
    val doseId: Int,
    val medId: Int,
    val userId: Int,
    val medicationName: String,
    val dosage: String?,
    val instructions: String?,
    val iconType: String?,
    val scheduledTime: String,
    val takenAt: String?,
    val status: DoseStatus,
    val snoozeCount: Int = 0,
    val doseDate: String,
    val reminderEnabled: Boolean = true
)