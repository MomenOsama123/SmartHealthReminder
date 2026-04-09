package com.example.smarthealthreminder.features.auth.signin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.smarthealthreminder.databinding.ActivityWelcomeBinding
import com.example.smarthealthreminder.databinding.LoginBinding
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.example.smarthealthreminder.features.welcome.WelcomeActivity2
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SigninActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var auth: FirebaseAuth
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

