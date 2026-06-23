package com.example.smarthealthreminder.features.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.ui.DashboardActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {

    fun setup(
        activity: Activity,
        bottomNavigation: BottomNavigationView,
        selectedItemId: Int? = null
    ) {
        selectedItemId?.let { bottomNavigation.selectedItemId = it }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    openDashboard(activity)
                    true
                }

                R.id.nav_schedule -> {
                    openMainDestination(activity, MainActivity.DESTINATION_SCHEDULE)
                    true
                }

                R.id.nav_create -> {
                    if (activity is AppCompatActivity) {
                        QuickActionsBottomSheet.newInstance()
                            .show(activity.supportFragmentManager, QuickActionsBottomSheet.TAG)
                    }
                    false
                }

                R.id.nav_ai -> {
                    if (activity !is ChatBotActivity) {
                        activity.startActivity(
                            Intent(activity, ChatBotActivity::class.java)
                        )
                    }
                    true
                }

                R.id.action_settings -> {
                    if (activity !is SettingsActivity) {
                        activity.startActivity(
                            Intent(activity, SettingsActivity::class.java)
                        )
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun openMainDestination(activity: Activity, destination: String) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(intent)
    }

    private fun openDashboard(activity: Activity) {
        // ✅ Check if logged in
        val sharedPref = activity.getSharedPreferences("HealthSyncPrefs", Context.MODE_PRIVATE)
        val firebaseId = sharedPref.getString("FIREBASE_ID", "") ?: ""

        if (firebaseId.isEmpty()) {
            // Not logged in → go to Login
            val intent = Intent(activity, SignInActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
            return
        }

        if (activity is DashboardActivity) return

        // ✅ Simple start without flags
        activity.startActivity(Intent(activity, DashboardActivity::class.java))
    }
}