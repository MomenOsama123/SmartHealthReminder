package com.example.smarthealthreminder.features.auth.signIn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.LoginBinding
import com.example.smarthealthreminder.features.auth.forget_password.ForgetPasswordActivity
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User

class SignInActivity : BaseActivity() {

    companion object {
        private const val TAG = "SignInActivity"
    }

    private lateinit var binding: LoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleAuthHelper: GoogleAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ❌ شيل الـ auto-redirect - Splash هو اللي يقرر
        // val sharedPref = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
        // val existingId = sharedPref.getString("FIREBASE_ID", "") ?: ""
        // if (existingId.isNotEmpty()) { ... }

        enableEdgeToEdge()
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        // ================= SIGN UP =================
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        // ================= LOGIN =================
        binding.btnLogin.setOnClickListener {
            if (validateForm()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        setLoading(false)
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            val firebaseId = firebaseUser?.uid ?: ""

                            Log.d(TAG, "Login success, firebaseId: '$firebaseId'")

                            if (firebaseId.isEmpty()) {
                                showSnakeBar("Error: No user ID")
                                return@addOnCompleteListener
                            }

                            // ✅ SAVE SESSION
                            getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("FIREBASE_ID", firebaseId)
                                .apply()

                            // ✅ INSERT USER IN SQLITE
                            val db = DatabaseHelper(this)
                            val existingUser = db.getUserByFirebaseId(firebaseId)

                            if (existingUser == null) {
                                val user = User(
                                    firebaseId = firebaseId,
                                    name = firebaseUser?.displayName ?: "User",
                                    email = firebaseUser?.email ?: email
                                )
                                db.insertUser(user)
                                Log.d(TAG, "User inserted in SQLite: $firebaseId")
                            }

                            showSnakeBar("Welcome Back")

                            // ✅ روح Dashboard من غير flags
                            checkProfileCompletion(firebaseId)
                        } else {
                            Log.e(TAG, "Login failed: ${task.exception?.message}")
                            showSnakeBar("Login Failed: ${task.exception?.message}")
                        }
                    }
            }
        }

        // Toggle password visibility
        binding.icTogglePassword.setOnClickListener {
            val selection = binding.etPassword.selectionEnd
            val isPasswordVisible = binding.etPassword.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

            if (isPasswordVisible) {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }

            binding.etPassword.setSelection(selection)
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
            binding.etEmail.text.clear()
            binding.etPassword.text.clear()
        }

        // Google Authentication
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, errorMessage ->
            if (isSuccess) {
                val firebaseUser = auth.currentUser
                val firebaseId = firebaseUser?.uid ?: ""

                Log.d(TAG, "Google login success, firebaseId: '$firebaseId'")

                if (firebaseId.isEmpty()) {
                    setLoading(false)
                    // Toast removed
                    return@GoogleAuthHelper
                }

                // ✅ SAVE SESSION
                getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("FIREBASE_ID", firebaseId)
                    .apply()

                // ✅ INSERT USER IN SQLITE
                val db = DatabaseHelper(this)
                val existingUser = db.getUserByFirebaseId(firebaseId)

                if (existingUser == null) {
                    val user = User(
                        firebaseId = firebaseId,
                        name = firebaseUser?.displayName ?: "User",
                        email = firebaseUser?.email ?: ""
                    )
                    db.insertUser(user)
                    Log.d(TAG, "Google user inserted in SQLite: $firebaseId")
                }


                // ✅ روح Dashboard من غير flags
                checkProfileCompletion(firebaseId)
            } else {
                setLoading(false)
                Log.e(TAG, "Google login failed: $errorMessage")
                showSnakeBar(
                    errorMessage ?: "An error occurred during Google login",
                    Snackbar.LENGTH_LONG
                )
            }
        }

        // Google button
        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleAuthHelper.startLogin()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }

    private fun validateForm(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        var isValid = true

        if (email.isEmpty()) {
            binding.etEmail.error = "Required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.etEmail.error = null
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Required"
            isValid = false
        } else {
            binding.etPassword.error = null
        }

        return isValid
    }

    private fun checkProfileCompletion(uid: String) {
        setLoading(true)
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                setLoading(false)
                // Robust check: try multiple field names and object mapping
                val firestoreCompleted = document.getBoolean("isProfileCompleted") ?: 
                                       document.getBoolean("profileCompleted") ?: 
                                       (document.toObject(User::class.java)?.isProfileCompleted ?: false)
                
                // Fallback to local storage if Firestore is uncertain
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                
                val isCompleted = firestoreCompleted || localCompleted
                
                // Sync local storage if Firestore confirms completion
                getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isProfileCompleted", isCompleted)
                    .apply()

                val intent = if (isCompleted) {
                    Intent(this, com.example.smarthealthreminder.features.activity.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                } else {
                    Intent(this, com.example.smarthealthreminder.features.auth.signup.CompleteProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                // Fallback to local on network error
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                
                val intent = if (localCompleted) {
                    Intent(this, com.example.smarthealthreminder.features.activity.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                } else {
                    Intent(this, com.example.smarthealthreminder.features.auth.signup.CompleteProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
                startActivity(intent)
                finish()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
    }
}