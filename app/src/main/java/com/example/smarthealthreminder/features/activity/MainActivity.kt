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
import com.example.smarthealthreminder.features.settings.SettingsFragment
import com.example.smarthealthreminder.features.stepsTracker.StepsTrackerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        const val DESTINATION_REMINDERS = "reminders"
        const val DESTINATION_SETTINGS = "settings"
        const val DESTINATION_INSIGHTS = "insights"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
        private const val TAG_HOME = "home"
        private const val TAG_SCHEDULE = "schedule"
        private const val TAG_ALARMS = "alarms"
        private const val TAG_REMINDERS = "reminders"
        private const val TAG_SETTINGS = "settings"
        private const val TAG_INSIGHTS = "insights"
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private lateinit var remindersFragment: RemindersFragment
    private lateinit var chatBotFragment: ChatBotFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var stepsTrackerFragment: StepsTrackerFragment
    private lateinit var bottomNavigation: BottomNavigationView
    private var activeFragment: Fragment? = null
    private var isProgrammaticSelection = false

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
            settingsFragment = SettingsFragment()
            stepsTrackerFragment = StepsTrackerFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, scheduleFragment, TAG_SCHEDULE).hide(scheduleFragment)
                .add(R.id.fragment_container, alarmsFragment, TAG_ALARMS).hide(alarmsFragment)
                .add(R.id.fragment_container, remindersFragment, TAG_REMINDERS).hide(remindersFragment)
                .add(R.id.fragment_container, chatBotFragment, "chatbot").hide(chatBotFragment)
                .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                .add(R.id.fragment_container, stepsTrackerFragment, TAG_INSIGHTS).hide(stepsTrackerFragment)
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()

            activeFragment = homeFragment
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment ?: HomeFragment()
            scheduleFragment = supportFragmentManager.findFragmentByTag(TAG_SCHEDULE) as? ScheduleFragment ?: ScheduleFragment()
            alarmsFragment = supportFragmentManager.findFragmentByTag(TAG_ALARMS) as? AlarmsFragment ?: AlarmsFragment()
            remindersFragment = supportFragmentManager.findFragmentByTag(TAG_REMINDERS) as? RemindersFragment ?: RemindersFragment()
            chatBotFragment = supportFragmentManager.findFragmentByTag("chatbot") as? ChatBotFragment ?: ChatBotFragment()
            settingsFragment = supportFragmentManager.findFragmentByTag(TAG_SETTINGS) as? SettingsFragment ?: SettingsFragment()
            stepsTrackerFragment = supportFragmentManager.findFragmentByTag(TAG_INSIGHTS) as? StepsTrackerFragment ?: StepsTrackerFragment()
            activeFragment = supportFragmentManager.fragments.find { it.isAdded && !it.isHidden } ?: homeFragment
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_schedule -> showFragment(scheduleFragment)
                R.id.nav_create -> {
                    if (!isProgrammaticSelection) {
                        QuickActionsBottomSheet.newInstance().show(supportFragmentManager, QuickActionsBottomSheet.TAG)
                    }
                    true
                }
                R.id.nav_ai -> showFragment(chatBotFragment)
                R.id.action_settings -> showFragment(settingsFragment)
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

        if (savedInstanceState == null) {
            val destination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME
            navigateToDestination(destination)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // اقفل أي BottomSheet مفتوح
        (supportFragmentManager.findFragmentByTag(QuickActionsBottomSheet.TAG)
                as? QuickActionsBottomSheet)?.dismissAllowingStateLoss()

        val destination = intent.getStringExtra(EXTRA_START_DESTINATION)
            ?: DESTINATION_HOME

        navigateToDestination(destination)
    }

    fun navigateToDestination(destination: String) {
        isProgrammaticSelection = true
        when (destination) {
            DESTINATION_SCHEDULE -> {
                if (bottomNavigation.selectedItemId != R.id.nav_schedule) {
                    bottomNavigation.selectedItemId = R.id.nav_schedule
                }
                showFragment(scheduleFragment)
            }
            DESTINATION_ALARMS -> {
                showFragment(alarmsFragment)
            }

            DESTINATION_REMINDERS -> {
                showFragment(remindersFragment)
            }
            "chatbot", "ai" -> {
                if (bottomNavigation.selectedItemId != R.id.nav_ai) {
                    bottomNavigation.selectedItemId = R.id.nav_ai
                }
                showFragment(chatBotFragment)
            }
            DESTINATION_SETTINGS -> {
                if (bottomNavigation.selectedItemId != R.id.action_settings) {
                    bottomNavigation.selectedItemId = R.id.action_settings
                }
                showFragment(settingsFragment)
            }
            DESTINATION_INSIGHTS -> {
                showFragment(stepsTrackerFragment)
            }
            else -> {
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                }
                showFragment(homeFragment)
            }
        }
        isProgrammaticSelection = false
    }

    private fun showFragment(fragment: Fragment): Boolean {
        if (activeFragment == fragment) return true
        
        val transaction = supportFragmentManager.beginTransaction()
        
        // Safety check for activeFragment being null
        activeFragment?.let {
            transaction.hide(it)
        }
        
        transaction.show(fragment).commit()
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