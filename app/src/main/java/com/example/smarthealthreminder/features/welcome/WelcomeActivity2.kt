package com.example.smarthealthreminder.features.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.ActivityWelcome2Binding
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.auth.signup.SignupActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity2 : AppCompatActivity() {

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
                Snackbar.make(binding.root, "Successfully logged in via Google!", Snackbar.LENGTH_LONG).show()
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
}

