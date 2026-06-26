package com.example.smarthealthreminder.features.navigation

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.main.MainWelcomeActivity
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Navigation helper for bottom navigation bar across all activities.
 * The create (+) action is always global and static: it shows the QuickActionsBottomSheet
 * on every screen for consistent behavior.
 */
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
                    openHome(activity)
                    true
                }
                R.id.nav_schedule -> {
                    openMainDestination(activity, MainActivity.DESTINATION_SCHEDULE)
                    true
                }
                R.id.nav_create -> {
                    if (activity is AppCompatActivity) {
                        QuickActionsBottomSheet().show(
                            activity.supportFragmentManager,
                            QuickActionsBottomSheet.TAG
                        )
                    }
                    false
                }
                R.id.nav_ai -> {
                    if (activity is ChatBotActivity) {
                        true
                    } else {
                        activity.startActivity(Intent(activity, ChatBotActivity::class.java))
                        false
                    }
                }
                R.id.action_settings -> {
                    if (activity is SettingsActivity) {
                        true
                    } else {
                        activity.startActivity(Intent(activity, SettingsActivity::class.java))
                        activity.finish()
                        true
                    }
                }
                else -> false
            }
        }
    }

    private fun openMainDestination(activity: Activity, destination: String) {
        activity.startActivity(Intent(activity, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        activity.finish()
    }

    private fun openDashboard(activity: Activity) {
        // Check live Firebase session — not just cached UID
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            activity.startActivity(Intent(activity, SignInActivity::class.java))
            activity.finish()
            return
        }

        if (activity is DashboardActivity) return
        activity.startActivity(Intent(activity, DashboardActivity::class.java))
    }
}
