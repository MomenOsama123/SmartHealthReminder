package com.example.smarthealthreminder.features.model_dashboard

import com.google.firebase.firestore.PropertyName

data class User(
    val id: Int = -1,
    val firebaseId: String = "",
    val name: String = "",
    val email: String = "",
    val dob: String = "",
    val gender: String = "",
    val bloodType: String = "",
    val weight: String = "",
    val height: String = "",
    val chronicDiseases: String = "",
    val allergies: String = "",
    val emergencyContact: String = "",
    val profileImage: String? = null,
    val dailyStepGoal: Int = 10000,
    val dailyWaterGoal: Int = 2000,
    val sleepGoalHours: Double = 8.0,
    @get:PropertyName("isProfileCompleted")
    @set:PropertyName("isProfileCompleted")
    var isProfileCompleted: Boolean = false
)
