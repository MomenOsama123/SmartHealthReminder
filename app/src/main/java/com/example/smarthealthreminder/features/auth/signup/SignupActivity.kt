package com.example.smarthealthreminder.features.auth.signup

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.model_dashboard.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : BaseActivity() {

    private lateinit var binding: SignupBinding
    private val viewModel: SignupViewModel by viewModels()
    private val auth by lazy { FirebaseAuth.getInstance() }
    
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
        binding.tvSwitchAuth.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString()
                viewModel.signUp(email, password)
            }
        }

        // Clear error indicators when user starts typing
        binding.etEmail.addTextChangedListener { binding.etEmail.error = null }
        binding.etPassword.addTextChangedListener { binding.etPassword.error = null }
        binding.etConfirmPassword.addTextChangedListener { binding.etConfirmPassword.error = null }

        // Password visibility toggle
        binding.icTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.icTogglePassword)
        }

        binding.icToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(binding.etConfirmPassword, binding.icToggleConfirmPassword)
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

    private fun setupObservers() {
        viewModel.signupState.observe(this) { state ->
            when (state) {
                is SignupState.Loading -> setLoading(true)
                is SignupState.Success -> {
                    setLoading(false)
                    checkProfileAndNavigate()
                }
                is SignupState.Error -> {
                    setLoading(false)
                    showSnakeBar("Sign Up Failed: ${state.message}")
                }
                is SignupState.Idle -> setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
    }

    private fun setupGoogleAuth() {
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, error ->
            if (isSuccess) {
                checkProfileAndNavigate()
            } else {
                setLoading(false)
                showSnakeBar(error ?: "Google Error")
            }
        }

        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleAuthHelper.startLogin()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val isChecked = binding.checkBox.isChecked

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!isChecked) {
            showSnakeBar("You must agree to the terms")
            isValid = false
        }

        return isValid
    }

    private fun checkProfileAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        saveSessionAndSync(uid, email)
    }

    private fun saveSessionAndSync(firebaseId: String, email: String) {
        getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE).edit {
            putString("FIREBASE_ID", firebaseId)
        }

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
                // Determine completion state based ONLY on Firestore for this account
                val isCompleted = if (document.exists()) {
                    document.getBoolean("isProfileCompleted") ?: 
                    document.getBoolean("profileCompleted") ?: 
                    (document.toObject(User::class.java)?.isProfileCompleted ?: false)
                } else {
                    false
                }

                // Update local storage to match this user's state
                getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isProfileCompleted", isCompleted)
                    .apply()

                setLoading(false)
                if (isCompleted) navigateToMain() else navigateToCompleteProfile()
            }
            .addOnFailureListener {
                // On failure, fallback to local state for this session
                val localCompleted = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
                    .getBoolean("isProfileCompleted", false)
                setLoading(false)
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
        val intent = Intent(this, CompleteProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
}
