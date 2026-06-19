package com.example.smarthealthreminder.features.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.features.Search.SearchActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.fragment.AlarmsFragment
import com.example.smarthealthreminder.features.fragment.HomeFragment
import com.example.smarthealthreminder.features.fragment.RemindersFragment
import com.example.smarthealthreminder.features.fragment.ScheduleFragment
import com.example.smarthealthreminder.features.main.MainWelcomeActivity
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        const val DESTINATION_REMINDER = "reminder"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private lateinit var remindersFragment: RemindersFragment
    private lateinit var bottomNavigation: BottomNavigationView
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        bottomNavigation = findViewById(R.id.bottom_navigation)
        val requestedDestination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            scheduleFragment = ScheduleFragment()
            alarmsFragment = AlarmsFragment()
            remindersFragment = RemindersFragment()
            activeFragment = when (requestedDestination) {
                DESTINATION_SCHEDULE -> scheduleFragment
                DESTINATION_ALARMS -> alarmsFragment
                DESTINATION_REMINDER -> remindersFragment
                else -> homeFragment
            }

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, homeFragment, "home")
                add(R.id.fragment_container, scheduleFragment, "schedule")
                add(R.id.fragment_container, alarmsFragment, "alarms")
                add(R.id.fragment_container, remindersFragment, "reminders")
                listOf(homeFragment, scheduleFragment, alarmsFragment, remindersFragment)
                    .filter { it != activeFragment }
                    .forEach { hide(it) }
            }.commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            scheduleFragment = supportFragmentManager.findFragmentByTag("schedule") as? ScheduleFragment ?: ScheduleFragment()
            alarmsFragment = supportFragmentManager.findFragmentByTag("alarms") as? AlarmsFragment ?: AlarmsFragment()
            remindersFragment = supportFragmentManager.findFragmentByTag("reminders") as? RemindersFragment ?: RemindersFragment()
            activeFragment = listOf(homeFragment, scheduleFragment, alarmsFragment, remindersFragment)
                .firstOrNull { !it.isHidden }
                ?: homeFragment
        }

        selectStartDestination(bottomNavigation, requestedDestination)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainWelcomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }
                R.id.nav_schedule -> switchFragment(scheduleFragment)
                R.id.nav_create -> {
                    showQuickActions(bottomNavigation.findViewById(R.id.nav_create))
                    false
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, ChatBotActivity::class.java))
                    false
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val destination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME
        navigateToDestination(destination)
    }

    private fun selectStartDestination(bottomNavigation: BottomNavigationView, destination: String) {
        bottomNavigation.selectedItemId = when (destination) {
            DESTINATION_SCHEDULE -> R.id.nav_schedule
            else -> R.id.nav_home
        }
        navigateToDestination(destination)
    }

    private fun navigateToDestination(destination: String) {
        when (destination) {
            DESTINATION_SCHEDULE -> switchFragment(scheduleFragment)
            DESTINATION_ALARMS -> switchFragment(alarmsFragment)
            DESTINATION_REMINDER -> switchFragment(remindersFragment)
            else -> switchFragment(homeFragment)
        }
    }

    private fun switchFragment(fragment: Fragment): Boolean {
        if (activeFragment == fragment) return true

        supportFragmentManager.beginTransaction()
            .hide(activeFragment ?: homeFragment)
            .show(fragment)
            .commit()

        activeFragment = fragment
        return true
    }

    private fun showQuickActions(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.quick_actions_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.reminder -> {
                        switchFragment(remindersFragment)
                        true
                    }
                    R.id.action_alarms -> {
                        switchFragment(alarmsFragment)
                        true
                    }
                    R.id.action_reports -> {
                        startActivity(Intent(this@MainActivity, ReportsActivity::class.java))
                        true
                    }

                    else -> false
                }
            }
            show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }
}
