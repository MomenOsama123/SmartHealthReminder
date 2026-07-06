package com.example.smarthealthreminder.features.Profileinfo.reports

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.example.smarthealthreminder.features.util.ImageUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val localDb by lazy { DatabaseHelper(this) }
    private var selectedImageUri: Uri? = null

    private val viewModel: HealthViewModel by viewModels {
        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

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
    private lateinit var profileImage: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            profileImage.setImageURI(it)
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            profileImage.setImageBitmap(it)
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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(SettingsPrefs.getSavedNightMode(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
        profileImage = findViewById(R.id.profile_image)

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
                        
                        it.profileImage?.let { base64 ->
                            val bitmap = ImageUtils.base64ToBitmap(base64)
                            profileImage.setImageBitmap(bitmap)
                        }
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

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.editProfileCard).setOnClickListener {
            showImageSourceDialog()
        }

        profileImage.setOnClickListener {
            showImageSourceDialog()
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

        val profileImageBase64 = try {
            val bitmap = profileImage.drawable.toBitmap()
            ImageUtils.bitmapToBase64(bitmap)
        } catch (e: Exception) {
            null
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
            profileImage = profileImageBase64,
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

                viewModel.updateCurrentUser(updatedUser)

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
        MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
            .setTitle("Select Gender")
            .setItems(options) { _, which ->
                editText.setText(options[which])
            }
            .show()
    }

    private fun showBloodTypeDialog(editText: EditText) {
        val options = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
        MaterialAlertDialogBuilder(this, R.style.AppAlertDialogTheme)
            .setTitle("Select Blood Type")
            .setItems(options) { _, which ->
                editText.setText(options[which])
            }
            .show()
    }
}
