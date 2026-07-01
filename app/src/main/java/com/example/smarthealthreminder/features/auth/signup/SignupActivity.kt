package com.example.smarthealthreminder.features.auth.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

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
                    binding.signupBtn.isEnabled = false
                    // If you add a progress bar to your layout, show it here
                }
                is SignupState.Success -> {
                    binding.signupBtn.isEnabled = true
                    showSnackbar("Sign Up Success")
                    navigateToCompleteProfile()
                }
                is SignupState.Error -> {
                    binding.signupBtn.isEnabled = true
                    showSnackbar("Sign Up Failed: ${state.message}")
                }
                is SignupState.Idle -> {
                    binding.signupBtn.isEnabled = true
                }
            }
        }
    }

    private fun setupGoogleAuth() {
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, error ->
            if (isSuccess) {
                showSnackbar("Google Sign Up Success")
                navigateToCompleteProfile()
            } else {
                showSnackbar(error ?: "Google Error")
            }
        }

        // Use findViewWithTag as in original code or add an ID to the button in signup.xml
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
