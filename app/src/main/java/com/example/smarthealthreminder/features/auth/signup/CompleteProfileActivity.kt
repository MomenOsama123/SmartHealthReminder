package com.example.smarthealthreminder.features.auth.signup

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import java.util.Calendar

class CompleteProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)

        // 1. Initialize Views from XML
        val etDob = findViewById<EditText>(R.id.et_dob)
        val tvGender = findViewById<TextView>(R.id.tv_gender)
        val tvBloodType = findViewById<TextView>(R.id.tv_blood_type)
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        val btnSkip = findViewById<TextView>(R.id.btn_skip)

        // 2. Setup DatePicker for Date of Birth
        etDob.setOnClickListener {
            showDatePickerDialog(etDob)
        }

        // 3. Setup Gender selection dialog
        tvGender.setOnClickListener {
            val options = arrayOf("Female", "Male", "Non-Binary", "Other")
            AlertDialog.Builder(this)
                .setTitle("Select Gender")
                .setItems(options) { _, which -> tvGender.text = options[which] }
                .show()
        }

        // 4. Setup Blood Type selection dialog
        tvBloodType.setOnClickListener {
            val options = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
            AlertDialog.Builder(this)
                .setTitle("Select Blood Type")
                .setItems(options) { _, which -> tvBloodType.text = options[which] }
                .show()
        }

//        // 5. Continue button logic
//        btnContinue.setOnClickListener {
//            // Here you should save the user's data to your database
//
//            // Save in SharedPreferences that the profile is completed so this screen won't show again
//            val sharedPreferences = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
//            val editor = sharedPreferences.edit()
//            editor.putBoolean("isProfileCompleted", true)
//            editor.apply()
//
//            Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
//            navigateToMain()
//        }

//        // 6. Skip button logic
//        btnSkip.setOnClickListener {
//            // Skipping navigates to the main screen without saving
//            // This ensures the screen will appear again next time the app is opened to complete the profile
//            Toast.makeText(this, "Profile setup skipped", Toast.LENGTH_SHORT).show()
//            navigateToMain()
//        }

        // 1. Initialize the ScrollView from XML
        val scrollView = findViewById<ScrollView>(R.id.complateInfo) // Ensure you have assigned this ID to the ScrollView in your XML

        // 2. Setup auto-scroll logic for the EditTexts
        // We will apply this to the weight, height, and all the bottom fields
        val bottomFields = listOf(
            findViewById<EditText>(R.id.et_weight),            // Added Weight
            findViewById<EditText>(R.id.et_height),            // Added Height
            findViewById<EditText>(R.id.et_chronic_diseases),
            findViewById<EditText>(R.id.et_allergies),
            findViewById<EditText>(R.id.et_emergency_contact)
        )

        bottomFields.forEach { editText ->
            editText?.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // When the field is clicked/focused, we wait 200 milliseconds (to give the keyboard time to open)
                    // Then we tell the ScrollView to smoothly scroll down to the bottom of this specific field
                    scrollView.postDelayed({
                        scrollView.smoothScrollTo(0, view.bottom)
                    }, 200)
                }
            }
        }
    }

    // ================= Helper Functions =================

    // Function to open the DatePickerDialog
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Date format: Month / Day / Year
            val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            editText.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

//    // Function to navigate to the Main Screen (Dashboard)
//    private fun navigateToMain() {
//        //  Make sure to change MainActivity to your actual main screen name
//        val intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
//
//        // Finish this activity so the user can't navigate back to it using the device's back button
//        finish()
//    }
}