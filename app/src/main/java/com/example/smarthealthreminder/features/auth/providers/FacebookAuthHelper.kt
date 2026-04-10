package com.example.smarthealthreminder.features.auth.providers

import android.app.Activity
import android.content.Intent
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth

/**
 * A helper class designed to handle all Facebook login and Firebase authentication logic.
 * This keeps the UI Activities clean and follows the "Separation of Concerns" principle.
 */
class FacebookAuthHelper(
    // The Activity context required by Facebook and Firebase SDKs to show UI or handle tasks
    private val activity: Activity,

    // The Firebase Authentication instance used to register the user
    private val auth: FirebaseAuth,

    // A callback function to send the final result (Success state, Error Message) back to the UI
    private val onAuthResult: (Boolean, String?) -> Unit
) {
    // CallbackManager acts as a bridge. It receives the raw data from the Facebook login window
    // and routes it to the appropriate function below (onSuccess, onCancel, onError).
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    // The 'init' block runs immediately when an instance of this class is created.
    init {
        // We register a callback listener to catch the outcome of the Facebook login process.
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            // Triggered if the user successfully logs into Facebook and grants permissions.
            override fun onSuccess(result: LoginResult) {
                // We take the valid Facebook Access Token and pass it to Firebase for final authentication.
                handleFacebookAccessToken(result.accessToken)
            }

            // Triggered if the user clicks the "X" or closes the Facebook login window before finishing.
            override fun onCancel() {
                // Return 'false' and a cancelation message back to the UI.
                onAuthResult(false, "Facebook login was canceled.")
            }

            // Triggered if there is a technical error, network issue, or invalid App ID configuration.
            override fun onError(error: FacebookException) {
                // Return 'false' and the specific Facebook error message back to the UI.
                onAuthResult(false, "Facebook login error: ${error.message}")
            }
        })
    }

    /**
     * This function should be called when the user clicks the "Login with Facebook" button.
     * It requests the necessary permissions ("email" and "public_profile") and opens the Facebook UI.
     */
    fun startLogin() {
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    /**
     * This function is CRITICAL. It must be called inside the Activity's `onActivityResult`.
     * It takes the raw background data returned by the Android system after the Facebook window closes
     * and hands it over to the 'callbackManager' to decode it.
     */
    fun forwardActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * This is the final step. It takes the valid Facebook token, converts it into a Firebase credential,
     * and attempts to sign the user into your Firebase project database.
     */
    private fun handleFacebookAccessToken(token: AccessToken) {
        // Convert the Facebook token into a format that Firebase understands
        val credential = FacebookAuthProvider.getCredential(token.token)

        // Attempt to sign in (or sign up) the user in Firebase using this credential
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                // Success: Send 'true' and 'null' (no errors) back to the UI
                onAuthResult(true, null)
            } else {
                // Failure: Send 'false' and the specific Firebase error message back to the UI
                onAuthResult(false, "Firebase authentication failed: ${task.exception?.message}")
            }
        }
    }
}