package com.example.smarthealthreminder.features.auth.forget_password


import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.databinding.ForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ForgetPasswordBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.backArrow.setOnClickListener {
            finish()
        }
        binding.sendButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            if (validateEmail(email)) {
                sendResetEmail(email)
            }
        }
    }
    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            return false
        }

        return true
    }
    private fun sendResetEmail(email: String) {
        binding.sendButton.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.sendButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Check your email to reset password",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Something went wrong",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

