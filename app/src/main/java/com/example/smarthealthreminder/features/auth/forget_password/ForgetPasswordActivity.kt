package com.example.smarthealthreminder.features.auth.forget_password


import android.os.Bundle
import android.widget.Toast
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.databinding.ForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : BaseActivity() {
    private lateinit var binding: ForgetPasswordBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
                        getString(R.string.check_your_email),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: getString(R.string.offline_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

