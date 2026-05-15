package com.example.smarthealthreminder.features.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}

class SignupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _signupState = MutableLiveData<SignupState>(SignupState.Idle)
    val signupState: LiveData<SignupState> get() = _signupState

    fun signUp(email: String, pass: String) {
        _signupState.value = SignupState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _signupState.value = SignupState.Success
                } else {
                    val errorMsg = task.exception?.message ?: "An unknown error occurred"
                    _signupState.value = SignupState.Error(errorMsg)
                }
            }
    }
}
