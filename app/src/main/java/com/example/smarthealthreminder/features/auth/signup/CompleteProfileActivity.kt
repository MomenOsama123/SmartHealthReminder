package com.example.smarthealthreminder.features.auth.signup

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.ActivityCompleteProfileBinding
import com.example.smarthealthreminder.features.data_d.DatabaseHelper
import com.example.smarthealthreminder.features.model_d.User
import com.example.smarthealthreminder.features.util.ImageUtils
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val localDb by lazy { DatabaseHelper(this) }
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.imgProfile.setImageURI(it)
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            binding.imgProfile.setImageBitmap(it)
            // Note: For a real app, you'd save this bitmap to a file and get a URI
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Permissions are required to change profile image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make edit text unwritable via keyboard (matches ProfileActivity behavior)
        binding.etDob.showSoftInputOnFocus = false
        binding.etGender.showSoftInputOnFocus = false
        binding.etBloodType.showSoftInputOnFocus = false


        setupListeners()
        setupAutoScroll()
        setupImagePicker()
    }

    private fun setupImagePicker() {
        binding.imgProfile.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_source, null)

        view.findViewById<android.view.View>(R.id.btn_camera).setOnClickListener {
            checkCameraPermissionAndLaunch()
            bottomSheetDialog.dismiss()
        }

        view.findViewById<android.view.View>(R.id.btn_gallery).setOnClickListener {
            checkGalleryPermissionAndLaunch()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            captureImageLauncher.launch(null)
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun checkGalleryPermissionAndLaunch() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(arrayOf(permission))
        }
    }

    private fun setupListeners() {
        // 1. Setup DatePicker for Date of Birth
        binding.etDob.setOnClickListener {
            showDatePickerDialog(binding.etDob)
        }

        // 2. Setup Gender selection dialog
        binding.etGender.setOnClickListener {
            val options = arrayOf("Female", "Male", "Non-Binary", "Other")
            MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
                .setTitle("Select Gender")
                .setItems(options) { _, which -> 
                    binding.etGender.setText(options[which])
                }
                .show()
        }

        // 3. Setup Blood Type selection dialog
        binding.etBloodType.setOnClickListener {
            val options = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
            MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
                .setTitle("Select Blood Type")
                .setItems(options) { _, which -> 
                    binding.etBloodType.setText(options[which])
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
        val gender = binding.etGender.text.toString().trim()
        val bloodType = binding.etBloodType.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        
        // Optional fields
        val chronicDiseases = binding.etChronicDiseases.text.toString().trim()
        val allergies = binding.etAllergies.text.toString().trim()
        val emergencyContact = binding.etEmergencyContact.text.toString().trim()

        // Mandatory fields check
        if (fullName.isEmpty() || dob.isEmpty() || dob == getString(com.example.smarthealthreminder.R.string.date) || 
            gender.isEmpty() || bloodType.isEmpty()) {
            Toast.makeText(this, "Please fill all mandatory fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validations for weight and height (Matches ProfileActivity)
        if (weight.isNotEmpty()) {
            val weightVal = weight.toDoubleOrNull()
            if (weightVal == null || weightVal !in 10.0..500.0) {
                binding.etWeight.error = "Please enter a valid weight (10-500 kg)"
                binding.etWeight.requestFocus()
                return
            }
        } else {
            Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show()
            binding.etWeight.requestFocus()
            return
        }

        if (height.isNotEmpty()) {
            val heightVal = height.toDoubleOrNull()
            if (heightVal == null || heightVal !in 50.0..300.0) {
                binding.etHeight.error = "Please enter a valid height (50-300 cm)"
                binding.etHeight.requestFocus()
                return
            }
        } else {
            Toast.makeText(this, "Please enter your height", Toast.LENGTH_SHORT).show()
            binding.etHeight.requestFocus()
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
            "profileImage" to (try { ImageUtils.bitmapToBase64(binding.imgProfile.drawable.toBitmap()) } catch (e: Exception) { null }),
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
                    profileImage = userMap["profileImage"] as String?,
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
