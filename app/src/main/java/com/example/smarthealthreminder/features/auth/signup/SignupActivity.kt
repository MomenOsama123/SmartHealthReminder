package com.example.smarthealthreminder.features.auth.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : BaseActivity() {

    private lateinit var binding: SignupBinding
    private val viewModel: SignupViewModel by viewModels()
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    // Google Authentication
    private lateinit var googleAuthHelper: GoogleAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
        setupObservers()
        setupGoogleAuth()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.loginText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        binding.signupBtn.setOnClickListener {
            if (validateForm()) {
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString()
                viewModel.signUp(email, password)
            }
        }

        // Clear error indicators when user starts typing
        binding.emailInput.addTextChangedListener { binding.emailInput.error = null }
        binding.passwordInput.addTextChangedListener { binding.passwordInput.error = null }
        binding.confirmPasswordInput.addTextChangedListener { binding.confirmPasswordInput.error = null }

    }

    private fun setupObservers() {
        viewModel.signupState.observe(this) { state ->
            when (state) {
                is SignupState.Loading -> {
                    setLoading(true)
                }
                is SignupState.Success -> {
                    setLoading(false)
                    showSnackbar("Sign Up Success")
                    checkProfileAndNavigate()
                }
                is SignupState.Error -> {
                    setLoading(false)
                    showSnackbar("Sign Up Failed: ${state.message}")
                }
                is SignupState.Idle -> {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signupBtn.isEnabled = !isLoading
    }

    private fun setupGoogleAuth() {
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, error ->
            if (isSuccess) {
                showSnackbar("Google Sign Up Success")
                checkProfileAndNavigate()
            } else {
                showSnackbar(error ?: "Google Error")
            }
        }

        binding.root.findViewWithTag<View>("google_btn")?.setOnClickListener {
            googleAuthHelper.startLogin()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val isChecked = binding.checkBox.isChecked

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Enter a valid email address"
            isValid = false
        }

        if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords do not match"
            isValid = false
        }

        if (!isChecked) {
            showSnackbar("You must agree to the terms")
            isValid = false
        }

        return isValid
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
                if (document.exists()) {
                    val firestoreUser = document.toObject(User::class.java)
                    
                    // Prioritize the Boolean field directly, then the User object
                    val isCompleted = document.getBoolean("isProfileCompleted") ?: 
                                     document.getBoolean("profileCompleted") ?: 
                                     (firestoreUser?.isProfileCompleted ?: false)
                    
                    if (isCompleted) {
                        // Sync SQLite with full profile from Firestore
                        firestoreUser?.let {
                            val localDb = DatabaseHelper(this)
                            if (localDb.getUserByFirebaseId(uid) == null) {
                                localDb.insertUser(it)
                            } else {
                                localDb.updateUser(it)
                            }
                        }

                        // Sync local session
                        getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("isProfileCompleted", true)
                            .apply()
                        
                        navigateToMain()
                        return@addOnSuccessListener
                    }
                }
                
                // If document doesn't exist or isProfileCompleted is false
                navigateToCompleteProfile()
            }
            .addOnFailureListener {
                // Fallback to local status on failure
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                if (localCompleted) {
                    navigateToMain()
                } else {
                    navigateToCompleteProfile()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToCompleteProfile() {
        startActivity(Intent(this, CompleteProfileActivity::class.java))
        finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
}
