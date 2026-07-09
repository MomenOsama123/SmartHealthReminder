package com.example.smarthealthreminder.features.auth.signIn

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.LoginBinding
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.auth.forget_password.ForgetPasswordActivity
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signup.CompleteProfileActivity
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : BaseActivity() {

    companion object {
        private const val TAG = "SignInActivity"
    }

    private lateinit var binding: LoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleAuthHelper: GoogleAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
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
        binding.tvSwitchAuth.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseId = auth.currentUser?.uid ?: ""
                            saveSessionAndSync(firebaseId, email)
                        } else {
                            setLoading(false)
                            showSnakeBar("Login Failed: ${task.exception?.message}")
                        }
                    }
            }
        }

        binding.icTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.icTogglePassword)
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        val selection = editText.selectionEnd
        val isPasswordVisible = editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

        if (isPasswordVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility_off)
            imageView.contentDescription = getString(R.string.show_password)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility)
            imageView.contentDescription = getString(R.string.hide_password)
        }
        editText.setSelection(selection)
    }

    private fun setupGoogleAuth() {
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, errorMessage ->
            if (isSuccess) {
                val firebaseId = auth.currentUser?.uid ?: ""
                saveSessionAndSync(firebaseId, auth.currentUser?.email ?: "")
            } else {
                setLoading(false)
                showSnakeBar(errorMessage ?: "Google Login Failed")
            }
        }

        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleAuthHelper.startLogin()
        }
    }

    private fun saveSessionAndSync(firebaseId: String, email: String) {
        if (firebaseId.isEmpty()) {
            setLoading(false)
            showSnakeBar("Error: No user ID")
            return
        }

        getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
            .edit()
            .putString("FIREBASE_ID", firebaseId)
            .apply()

        val db = DatabaseHelper(this)
        if (db.getUserByFirebaseId(firebaseId) == null) {
            val user = User(
                firebaseId = firebaseId,
                name = auth.currentUser?.displayName ?: "User",
                email = email
            )
            db.insertUser(user)
        }
        
        checkProfileCompletion(firebaseId)
    }

    private fun checkProfileCompletion(uid: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Truth from Firestore for this specific account
                val firestoreCompleted = if (document.exists()) {
                    document.getBoolean("isProfileCompleted") ?: 
                    document.getBoolean("profileCompleted") ?: 
                    (document.toObject(User::class.java)?.isProfileCompleted ?: false)
                } else {
                    false
                }
                
                // Update local storage with the state of the current user
                getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isProfileCompleted", firestoreCompleted)
                    .apply()

                setLoading(false)
                if (firestoreCompleted) navigateToMain() else navigateToCompleteProfile()
            }
            .addOnFailureListener {
                // Fallback to local storage only if Firestore is unreachable
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                setLoading(false)
                if (localCompleted) navigateToMain() else navigateToCompleteProfile()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToCompleteProfile() {
        val intent = Intent(this, CompleteProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun validateForm(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        var isValid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Required"
            isValid = false
        }

        return isValid
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
}
