package com.example.smarthealthreminder.features.model_d

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
    @get:PropertyName("isProfileCompleted")
    @set:PropertyName("isProfileCompleted")
    var isProfileCompleted: Boolean = false
)
