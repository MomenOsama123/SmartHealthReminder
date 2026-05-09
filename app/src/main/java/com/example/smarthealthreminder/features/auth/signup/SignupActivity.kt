package com.example.smarthealthreminder.features.auth.signup

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.databinding.SignupBinding
import com.example.smarthealthreminder.features.Search.SearchActivity
import com.example.smarthealthreminder.features.auth.providers.GoogleAuthHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }

    private lateinit var binding: SignupBinding
    private lateinit var auth: FirebaseAuth
    //Google
    private lateinit var googleAuthHelper: GoogleAuthHelper



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginText.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            finish()
            startActivity(intent)
        }
        auth = FirebaseAuth.getInstance()
        binding.signupBtn.setOnClickListener {
            if (validateForm()) {
                val email = binding.emailInput.text.toString()
                val password = binding.passwordInput.text.toString()
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Snackbar.make(binding.root, "Sign Up Success", Snackbar.LENGTH_SHORT).show()

                    } else {
                        Snackbar.make(
                            binding.root,
                            "Sign Up Failed $it.message",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }




        googleAuthHelper = GoogleAuthHelper(this, auth) { isSuccess, error ->
            if (isSuccess){
                Snackbar.make(binding.root, "Google Sign Up Success", Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }else Snackbar.make(binding.root, error ?: "Google Error", Snackbar.LENGTH_SHORT).show()
        }

        // Adjust these IDs if they are different in your signup.xml
        binding.root.findViewWithTag<android.view.View>("google_btn")?.setOnClickListener { googleAuthHelper.startLogin() }
    }


    private fun validateForm(): Boolean {

        val fullName = binding.fullNameInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val isChecked = binding.checkBox.isChecked

        var isValid = true

        fun validateField(value: String, field: EditText): Boolean {
            return if (value.isEmpty()) {
                field.error = "Required"
                false
            } else {
                field.error = null
                true
            }
        }

        isValid = validateField(fullName, binding.fullNameInput) && isValid
        isValid = validateField(username, binding.usernameInput) && isValid
        isValid = validateField(email, binding.emailInput) && isValid
        isValid = validateField(password, binding.passwordInput) && isValid
        isValid = validateField(confirmPassword, binding.confirmPasswordInput) && isValid

        // Password match
        if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords do not match"
            isValid = false
        }

        // Checkbox
        if (!isChecked) {
            binding.checkBox.error = "Required"
            isValid = false
        } else {
            binding.checkBox.error = null
        }

        return isValid
    }
}

