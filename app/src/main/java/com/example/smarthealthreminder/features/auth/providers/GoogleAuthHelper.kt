package com.example.smarthealthreminder.features.auth.providers

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuthHelper(
    private val activity: Activity,
    private val auth: FirebaseAuth,
    private val onResult: (Boolean, String?) -> Unit
) {

    companion object {
        const val RC_SIGN_IN = 1001
    }

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Ensure this ID matches the one in your Firebase Console (Web Client ID)
            .requestIdToken(activity.getString(com.example.smarthealthreminder.R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun startLogin() {
        // Sign out first to ensure the account picker always appears
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    7 -> "Network Error: Please check your internet connection."
                    10 -> "Developer Error: Check if SHA-1 is added to Firebase and Web Client ID is correct."
                    12500 -> "Sign-in failed. Please ensure Google Play Services are up to date."
                    12501 -> "Sign-in cancelled by user."
                    else -> "Google Sign-In Error (${e.statusCode}): ${e.message}"
                }
                Log.e("GOOGLE_AUTH", errorMessage)
                onResult(false, errorMessage)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val idToken = account?.idToken
        if (idToken == null) {
            onResult(false, "Google ID Token is null")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Firebase authentication failed")
                }
            }
    }
}