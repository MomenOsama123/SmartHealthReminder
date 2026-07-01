package com.example.smarthealthreminder.features.Profileinfo.reports

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data_d.DatabaseHelper
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.example.smarthealthreminder.features.model_d.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val localDb by lazy { DatabaseHelper(this) }

    private lateinit var fullNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var genderInput: EditText
    private lateinit var bloodTypeInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var diseasesInput: EditText
    private lateinit var allergiesInput: EditText
    private lateinit var emergencyContactInput: EditText
    private lateinit var userNameDisplay: TextView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(SettingsActivity.getSavedNightMode(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_profile_info)

        initViews()
        loadUserData()
        setupListeners()
    }

    private fun initViews() {
        fullNameInput = findViewById(R.id.full_name_input)
        dobInput = findViewById(R.id.dob_input)
        genderInput = findViewById(R.id.gender_input)
        bloodTypeInput = findViewById(R.id.blood_type_input)
        weightInput = findViewById(R.id.weight_input)
        heightInput = findViewById(R.id.height_input)
        diseasesInput = findViewById(R.id.diseases_input)
        allergiesInput = findViewById(R.id.allergies_input)
        emergencyContactInput = findViewById(R.id.emergency_contact_input)
        userNameDisplay = findViewById(R.id.tv_user_name)

        // make edit text unwritable
        dobInput.showSoftInputOnFocus = false
        genderInput.showSoftInputOnFocus = false
        bloodTypeInput.showSoftInputOnFocus = false
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        fullNameInput.setText(it.name)
                        userNameDisplay.text = it.name
                        dobInput.setText(it.dob)
                        genderInput.setText(it.gender)
                        bloodTypeInput.setText(it.bloodType)
                        weightInput.setText(it.weight)
                        heightInput.setText(it.height)
                        diseasesInput.setText(it.chronicDiseases)
                        allergiesInput.setText(it.allergies)
                        emergencyContactInput.setText(it.emergencyContact)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        dobInput.setOnClickListener {
            showDatePickerDialog(dobInput)
        }

        genderInput.setOnClickListener {
            showGenderDialog(genderInput)
        }

        bloodTypeInput.setOnClickListener {
            showBloodTypeDialog(bloodTypeInput)
        }

        findViewById<Button>(R.id.save_btn).setOnClickListener {
            saveUserData()
        }
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid ?: return

        val weightStr = weightInput.text.toString().trim()
        val heightStr = heightInput.text.toString().trim()

        if (weightStr.isNotEmpty()) {
            val weight = weightStr.toDoubleOrNull()
            if (weight == null || weight !in 10.0..500.0) {
                weightInput.error = "Please enter a valid weight (10-500 kg)"
                weightInput.requestFocus()
                return
            }
        }

        if (heightStr.isNotEmpty()) {
            val height = heightStr.toDoubleOrNull()
            if (height == null || height !in 50.0..300.0) {
                heightInput.error = "Please enter a valid height (50-300 cm)"
                heightInput.requestFocus()
                return
            }
        }

        val updatedUser = User(
            firebaseId = uid,
            name = fullNameInput.text.toString().trim(),
            email = auth.currentUser?.email ?: "",
            dob = dobInput.text.toString().trim(),
            gender = genderInput.text.toString().trim(),
            bloodType = bloodTypeInput.text.toString().trim(),
            weight = weightStr,
            height = heightStr,
            chronicDiseases = diseasesInput.text.toString().trim(),
            allergies = allergiesInput.text.toString().trim(),
            emergencyContact = emergencyContactInput.text.toString().trim(),
            isProfileCompleted = true
        )

        db.collection("users").document(uid)
            .set(updatedUser)
            .addOnSuccessListener {
                // Sync with Local Database
                val existingLocalUser = localDb.getUserByFirebaseId(uid)
                if (existingLocalUser != null) {
                    localDb.updateUser(updatedUser)
                } else {
                    localDb.insertUser(updatedUser)
                }

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                userNameDisplay.text = updatedUser.name
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            editText.setText(formattedDate)
        }, year, month, day).show()
    }

    private fun showGenderDialog(editText: EditText) {
        val options = arrayOf("Female", "Male", "Non-Binary", "Other")
        AlertDialog.Builder(this)
            .setTitle("Select Gender")
            .setItems(options) { _, which ->
                editText.setText(options[which])
            }
            .show()
    }

    private fun showBloodTypeDialog(editText: EditText) {
        val options = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
        AlertDialog.Builder(this)
            .setTitle("Select Blood Type")
            .setItems(options) { _, which ->
                editText.setText(options[which])
            }
            .show()
    }
}
