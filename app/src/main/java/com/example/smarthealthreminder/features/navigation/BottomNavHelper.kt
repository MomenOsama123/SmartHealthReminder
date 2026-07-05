package com.example.smarthealthreminder.features.navigation

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {

    fun setup(
        activity: Activity,
        bottomNavigation: BottomNavigationView,
        selectedItemId: Int? = null
    ) {
        selectedItemId?.let {
            bottomNavigation.selectedItemId = it
        }

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    openMainDestination(activity, MainActivity.DESTINATION_HOME)
                    true
                }

                R.id.nav_schedule -> {
                    if (activity !is MainActivity) {
                        openMainDestination(activity, MainActivity.DESTINATION_SCHEDULE)
                    }
                    true
                }

                R.id.nav_create -> {
                    if (activity is AppCompatActivity) {
                        QuickActionsBottomSheet.newInstance()
                            .show(activity.supportFragmentManager, QuickActionsBottomSheet.TAG)
                        true
                    } else {
                        false
                    }
                }

                R.id.nav_ai -> {
                    openMainDestination(activity, "chatbot")
                    true
                }

                R.id.action_settings -> {
                    if (activity !is SettingsPrefs) {
                        activity.startActivity(Intent(activity, SettingsPrefs::class.java))
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
}