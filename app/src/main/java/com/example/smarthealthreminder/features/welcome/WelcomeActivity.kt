package com.example.smarthealthreminder.features.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.ActivityWelcomeBinding
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.auth.signup.CompleteProfileActivity
import com.example.smarthealthreminder.features.model_dashboard.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private var isCheckingAuth = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen MUST be called before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2. Keep splash screen visible until we've decided where to navigate
        splashScreen.setKeepOnScreenCondition { isCheckingAuth }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Check profile completion status from Firestore
            checkProfileCompletion(currentUser.uid)
        } else {
            // No user logged in, show the welcome UI
            isCheckingAuth = false
            showWelcomeUI()
        }
    }

    private fun showWelcomeUI() {
        enableEdgeToEdge()
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signupBtn.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity2::class.java))
        }
        binding.signInBtn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }

    private fun checkProfileCompletion(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Robust check: try direct boolean read first, then object mapping
                val firestoreCompleted = document.getBoolean("isProfileCompleted") ?: 
                                       document.getBoolean("profileCompleted") ?: 
                                       (document.toObject(User::class.java)?.isProfileCompleted ?: false)
                
                // Fallback to local storage if Firestore is uncertain
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                
                val isCompleted = firestoreCompleted || localCompleted
                
                // Sync local storage if Firestore confirms completion
                if (firestoreCompleted && !localCompleted) {
                    getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isProfileCompleted", true)
                        .apply()
                }

                val intent = if (isCompleted) {
                    Intent(this, com.example.smarthealthreminder.features.activity.MainActivity::class.java)
                } else {
                    Intent(this, CompleteProfileActivity::class.java)
                }
                startActivity(intent)
                finish()
                isCheckingAuth = false
            }
            .addOnFailureListener {
                // Network failure fallback: check local storage
                val isCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                
                val intent = if (isCompleted) {
                    Intent(this, com.example.smarthealthreminder.features.activity.MainActivity::class.java)
                } else {
                    Intent(this, CompleteProfileActivity::class.java)
                }
                startActivity(intent)
                finish()
                isCheckingAuth = false
            }
    }
}
