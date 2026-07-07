package com.example.smarthealthreminder.features.profileInfo

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val localDb by lazy { DatabaseHelper(this) }

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
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
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

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        // Prefer local data for speed and offline support
        val localUser = localDb.getUserByFirebaseId(uid)
        if (localUser != null) {
            displayUser(localUser)
        }

        // Still fetch from Firestore to ensure synchronization
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        displayUser(it)
                        // Sync local DB
                        val existingLocalUser = localDb.getUserByFirebaseId(uid)
                        if (existingLocalUser != null) {
                            localDb.updateUser(it)
                        } else {
                            localDb.insertUser(it)
                        }
                    }
                }
            }
    }

    private fun displayUser(user: User) {
        fullNameInput.setText(user.name)
        userNameDisplay.text = user.name
        dobInput.setText(user.dob)
        genderInput.setText(user.gender)
        bloodTypeInput.setText(user.bloodType)
        weightInput.setText(user.weight)
        heightInput.setText(user.height)
        diseasesInput.setText(user.chronicDiseases)
        allergiesInput.setText(user.allergies)
        emergencyContactInput.setText(user.emergencyContact)

        user.profileImage?.let { base64 ->
            val bitmap = ImageUtils.base64ToBitmap(base64)
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap)
            }
        } ?: run {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.fab_edit_profile).setOnClickListener {
            val intent = android.content.Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }
}