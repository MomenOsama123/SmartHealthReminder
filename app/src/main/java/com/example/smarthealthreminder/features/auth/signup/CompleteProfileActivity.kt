package com.example.smarthealthreminder.features.auth.signup

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.databinding.ActivityCompleteProfileBinding
import com.example.smarthealthreminder.features.main.MainWelcomeActivity
import java.util.Calendar

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteProfileBinding

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

        // 5. Skip button logic
        binding.btnSkip.setOnClickListener {
            Toast.makeText(this, "Profile setup skipped", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    private fun setupAutoScroll() {
        // Setup auto-scroll logic for the bottom fields
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
        // Here you should save the user's data to your database (e.g., Room or Firebase)
        
        // Save in SharedPreferences that the profile is completed
        val sharedPreferences = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isProfileCompleted", true).apply()

        Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
        navigateToMain()
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
        val intent = Intent(this, MainWelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}