package com.example.smarthealthreminder.features.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.chatbot.ChatBotFragment
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.fragment.AlarmsFragment
import com.example.smarthealthreminder.features.fragment.HomeFragment
import com.example.smarthealthreminder.features.fragment.RemindersFragment
import com.example.smarthealthreminder.features.fragment.ScheduleFragment
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        const val DESTINATION_REMINDERS = "reminders"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
        private const val TAG_HOME = "home"
        private const val TAG_SCHEDULE = "schedule"
        private const val TAG_ALARMS = "alarms"
        private const val TAG_REMINDERS = "reminders"
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private lateinit var remindersFragment: RemindersFragment
    private lateinit var chatBotFragment: ChatBotFragment
    private lateinit var bottomNavigation: BottomNavigationView
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        bottomNavigation = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            scheduleFragment = ScheduleFragment()
            alarmsFragment = AlarmsFragment()
            remindersFragment = RemindersFragment()
            chatBotFragment = ChatBotFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, scheduleFragment, TAG_SCHEDULE).hide(scheduleFragment)
                .add(R.id.fragment_container, alarmsFragment, TAG_ALARMS).hide(alarmsFragment)
                .add(R.id.fragment_container, remindersFragment, TAG_REMINDERS).hide(remindersFragment)
                .add(R.id.fragment_container, chatBotFragment, "chatbot").hide(chatBotFragment)
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()

            activeFragment = homeFragment
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as HomeFragment
            scheduleFragment = supportFragmentManager.findFragmentByTag(TAG_SCHEDULE) as ScheduleFragment
            alarmsFragment = supportFragmentManager.findFragmentByTag(TAG_ALARMS) as AlarmsFragment
            remindersFragment = supportFragmentManager.findFragmentByTag(TAG_REMINDERS) as RemindersFragment
            chatBotFragment = supportFragmentManager.findFragmentByTag("chatbot") as ChatBotFragment
            activeFragment = supportFragmentManager.fragments.find { !it.isHidden } ?: homeFragment
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_schedule -> showFragment(scheduleFragment)
                R.id.nav_create -> {
                    QuickActionsBottomSheet.newInstance().show(supportFragmentManager, QuickActionsBottomSheet.TAG)
                    false
                }
                R.id.nav_ai -> showFragment(chatBotFragment)
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }

        // Handle Back button to return to Home fragment before exiting
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFragment != homeFragment) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Handle Keyboard visibility to hide/show Bottom Navigation
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            bottomNavigation.visibility = if (imeVisible) android.view.View.GONE else android.view.View.VISIBLE
            insets
        }

        val destination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME
        navigateToDestination(destination)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val destination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: return
        navigateToDestination(destination)
    }

    private fun navigateToDestination(destination: String) {
        when (destination) {
            DESTINATION_SCHEDULE -> {
                if (bottomNavigation.selectedItemId != R.id.nav_schedule) {
                    bottomNavigation.selectedItemId = R.id.nav_schedule
                }
                showFragment(scheduleFragment)
            }
            DESTINATION_ALARMS -> {
                if (bottomNavigation.selectedItemId != R.id.nav_schedule) {
                    bottomNavigation.selectedItemId = R.id.nav_schedule
                }
                showFragment(alarmsFragment)
            }
            DESTINATION_REMINDERS -> {
                if (bottomNavigation.selectedItemId != R.id.nav_schedule) {
                    bottomNavigation.selectedItemId = R.id.nav_schedule
                }
                showFragment(remindersFragment)
            }
            "chatbot", "ai" -> {
                if (bottomNavigation.selectedItemId != R.id.nav_ai) {
                    bottomNavigation.selectedItemId = R.id.nav_ai
                }
                showFragment(chatBotFragment)
            }
            else -> {
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                }
                showFragment(homeFragment)
            }
        }
    }

    private fun showFragment(fragment: Fragment): Boolean {
        if (activeFragment == fragment) return true
        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragment)
            .commit()
        activeFragment = fragment
        return true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }
}