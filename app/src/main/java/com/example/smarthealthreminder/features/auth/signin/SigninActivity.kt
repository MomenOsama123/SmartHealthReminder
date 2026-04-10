package com.example.smarthealthreminder.features.auth.signin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.smarthealthreminder.databinding.ActivityWelcomeBinding
import com.example.smarthealthreminder.databinding.LoginBinding
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.auth.providers.FacebookAuthHelper
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.example.smarthealthreminder.features.welcome.WelcomeActivity2
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SigninActivity : AppCompatActivity() {
    //for facebook auth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookAuthHelper.forwardActivityResult(requestCode, resultCode, data)
    }
    private lateinit var binding: LoginBinding
    private lateinit var auth: FirebaseAuth
    //Facebook
    private lateinit var facebookAuthHelper: FacebookAuthHelper

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
                            "Login Failed $it.message",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }


        }
        // --- Facebook Integration ---

        // 1. Initialize the Helper first and define what happens on success/failure
        facebookAuthHelper = FacebookAuthHelper(this, auth) { isSuccess, errorMessage ->
            if (isSuccess) {
                Toast.makeText(this, "Successfully logged in via Facebook!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, errorMessage ?: "An error occurred during login", Toast.LENGTH_LONG).show()
            }
        }

        // 2. Trigger login when the Facebook button is clicked
        binding.btnFacebook.setOnClickListener {
            facebookAuthHelper.startLogin()
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

