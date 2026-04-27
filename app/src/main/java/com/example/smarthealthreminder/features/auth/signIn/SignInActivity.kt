package com.example.smarthealthreminder.features.auth.signIn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.databinding.LoginBinding
import com.example.smarthealthreminder.features.auth.forget_password.ForgetPasswordActivity
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper


class SignInActivity : AppCompatActivity() {
    //for facebook auth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)

    }

    private lateinit var binding: LoginBinding
    private lateinit var auth: FirebaseAuth

    //Facebook
    private lateinit var googleAuthHelper: GoogleAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvCreateAccount.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            finish()
            startActivity(intent)
        }
        auth = FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener {
            if (validateForm()) {
                val email = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Snackbar.make(binding.root, "Welcome Back", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Login Failed: ${it.exception?.message}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }


        }
        // toggle password visibility
        binding.icTogglePassword.setOnClickListener {
            if (binding.etPassword.inputType == 1) {
                binding.etPassword.inputType = 129
            }
            else {
                binding.etPassword.inputType = 1
            }

        }
        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
            binding.etEmail.text.clear()
            binding.etPassword.text.clear()
        }


        // Initialize GoogleAuthHelper
        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, errorMessage ->
            if (isSuccess) {
                Toast.makeText(this, "Successfully logged in via Google!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this,
                    errorMessage ?: "An error occurred during Google login",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        //Google button
        binding.btnGoogle.setOnClickListener {
            googleAuthHelper.startLogin()
        }
    }


    private fun validateForm(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true

        if (email.isEmpty()) {
            binding.etEmail.error = "Required"
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
}

