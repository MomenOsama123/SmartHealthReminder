package com.example.smarthealthreminder.features.navigation

import android.app.Activity
import android.content.Intent
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.main.MainWelcomeActivity
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
                    openHome(activity)
                    true
                }
                R.id.nav_schedule -> {
                    openMainDestination(activity, MainActivity.DESTINATION_SCHEDULE)
                    true
                }
                R.id.nav_create -> {
                    activity.startActivity(Intent(activity, AddReminderActivity::class.java))
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

    private fun openHome(activity: Activity) {
        if (activity is MainWelcomeActivity) return

        activity.startActivity(Intent(activity, MainWelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        activity.finish()
    }
}
