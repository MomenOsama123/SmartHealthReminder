package com.example.smarthealthreminder.features.auth.signup

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.databinding.ActivityCompleteProfileBinding
import com.example.smarthealthreminder.features.data_d.DatabaseHelper
import com.example.smarthealthreminder.features.model_d.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val localDb by lazy { DatabaseHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupAutoScroll()
    }

    private fun setupListeners() {
        // 1. Setup DatePicker for Date of Birth
        binding.etDob.setOnClickListener {
            showDatePickerDialog(binding.etDob)
        }

        // 2. Setup Gender selection dialog
        binding.tvGender.setOnClickListener {
            val options = arrayOf("Female", "Male", "Non-Binary", "Other")
            AlertDialog.Builder(this)
                .setTitle("Select Gender")
                .setItems(options) { _, which -> 
                    binding.tvGender.text = options[which]
                    binding.tvGender.setTextColor(getColor(com.example.smarthealthreminder.R.color.text_dark))
                }
                .show()
        }

        // 3. Setup Blood Type selection dialog
        binding.tvBloodType.setOnClickListener {
            val options = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
            AlertDialog.Builder(this)
                .setTitle("Select Blood Type")
                .setItems(options) { _, which -> 
                    binding.tvBloodType.text = options[which]
                    binding.tvBloodType.setTextColor(getColor(com.example.smarthealthreminder.R.color.text_dark))
                }
                .show()
        }

        // 4. Continue button logic
        binding.btnContinue.setOnClickListener {
            saveProfileAndNavigate()
        }

        // Handle back press to force profile completion
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@CompleteProfileActivity, "Please complete your profile to continue", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAutoScroll() {
        val bottomFields = listOf(
            binding.etWeight,
            binding.etHeight,
            binding.etChronicDiseases,
            binding.etAllergies,
            binding.etEmergencyContact
        )

        bottomFields.forEach { editText ->
            editText.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    binding.complateInfo.postDelayed({
                        binding.complateInfo.smoothScrollTo(0, view.bottom)
                    }, 200)
                }
            }
        }
    }

    private fun saveProfileAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val fullName = binding.etFullName.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()
        val gender = binding.tvGender.text.toString().trim()
        val bloodType = binding.tvBloodType.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        
        // Optional fields
        val chronicDiseases = binding.etChronicDiseases.text.toString().trim()
        val allergies = binding.etAllergies.text.toString().trim()
        val emergencyContact = binding.etEmergencyContact.text.toString().trim()

        if (fullName.isEmpty() || dob == "mm/dd/yyyy" || gender == "Gender" || bloodType == "Blood Type" || weight.isEmpty() || height.isEmpty()) {
            Toast.makeText(this, "Please fill all mandatory fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userMap = hashMapOf(
            "firebaseId" to uid,
            "name" to fullName,
            "email" to auth.currentUser?.email.orEmpty(),
            "dob" to dob,
            "gender" to gender,
            "bloodType" to bloodType,
            "weight" to weight,
            "height" to height,
            "chronicDiseases" to chronicDiseases,
            "allergies" to allergies,
            "emergencyContact" to emergencyContact,
            "isProfileCompleted" to true
        )

        binding.btnContinue.isEnabled = false
        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                // Sync with Local Database
                val userProfile = User(
                    firebaseId = uid,
                    name = fullName,
                    email = auth.currentUser?.email.orEmpty(),
                    dob = dob,
                    gender = gender,
                    bloodType = bloodType,
                    weight = weight,
                    height = height,
                    chronicDiseases = chronicDiseases,
                    allergies = allergies,
                    emergencyContact = emergencyContact,
                    isProfileCompleted = true
                )
                
                val existingLocalUser = localDb.getUserByFirebaseId(uid)
                if (existingLocalUser != null) {
                    localDb.updateUser(userProfile)
                } else {
                    localDb.insertUser(userProfile)
                }

                getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isProfileCompleted", true)
                    .apply()
                
                Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                binding.btnContinue.isEnabled = true
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
            println("--------------------------------Failed to save profile: ${e.message}")
            }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            editText.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, com.example.smarthealthreminder.features.activity.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
