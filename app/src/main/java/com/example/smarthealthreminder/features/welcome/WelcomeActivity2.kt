package com.example.smarthealthreminder.features.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.ActivityWelcome2Binding
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.example.smarthealthreminder.features.auth.signup.CompleteProfileActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity2 : BaseActivity() {

    private lateinit var binding: ActivityWelcome2Binding
    private lateinit var auth: FirebaseAuth // Required for Firebase Auth
    private lateinit var googleAuthHelper: GoogleAuthHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWelcome2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance() // Initialize Firebase

        // Initialize Facebook Helper and handle the callback result

        googleAuthHelper = GoogleAuthHelper(this, auth){isSuccess, errorMessage ->
            if (isSuccess) {
                checkProfileAndNavigate()
            } else {
                Snackbar.make(binding.root, errorMessage ?: "An error occurred during login", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.signupBtn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        binding.loginTxt.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        binding.signupGoogle.setOnClickListener {
            googleAuthHelper.startLogin()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result back to the GoogleAuthHelper SDK
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }

    private fun checkProfileAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Sync with local session
        getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
            .edit()
            .putString("FIREBASE_ID", uid)
            .apply()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val firestoreUser = try {
                    if (document.exists()) document.toObject(User::class.java) else null
                } catch (e: Exception) {
                    null
                }

                val firestoreCompleted = document.getBoolean("isProfileCompleted") ?:
                                       document.getBoolean("profileCompleted") ?:
                                       (firestoreUser?.isProfileCompleted ?: false)
                
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                
                val isCompleted = firestoreCompleted || localCompleted
                
                if (isCompleted) {
                    // Sync SQLite with full profile if we got it from Firestore
                    firestoreUser?.let {
                        val localDb = DatabaseHelper(this)
                        if (localDb.getUserByFirebaseId(uid) == null) {
                            localDb.insertUser(it)
                        } else {
                            localDb.updateUser(it)
                        }
                    }

                    getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isProfileCompleted", true)
                        .apply()
                    navigateToMain()
                } else {
                    // Ensure minimal record in local DB for incomplete profile
                    val localDb = DatabaseHelper(this)
                    if (localDb.getUserByFirebaseId(uid) == null) {
                        val firebaseUser = auth.currentUser
                        val minimalUser = User(
                            firebaseId = uid,
                            name = firebaseUser?.displayName ?: "User",
                            email = firebaseUser?.email ?: ""
                        )
                        localDb.insertUser(minimalUser)
                    }
                    navigateToCompleteProfile()
                }
            }
            .addOnFailureListener {
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                if (localCompleted) navigateToMain() else navigateToCompleteProfile()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToCompleteProfile() {
        val intent = Intent(this, CompleteProfileActivity::class.java)
        startActivity(intent)
        finish()
    }
}

