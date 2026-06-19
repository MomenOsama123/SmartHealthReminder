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
import com.example.smarthealthreminder.features.fragment.ScheduleFragment
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val requestedDestination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            scheduleFragment = ScheduleFragment()
            alarmsFragment = AlarmsFragment()
            activeFragment = when (requestedDestination) {
                DESTINATION_SCHEDULE -> scheduleFragment
                DESTINATION_ALARMS -> alarmsFragment
                else -> homeFragment
            }

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, homeFragment, "home")
                add(R.id.fragment_container, scheduleFragment, "schedule")
                add(R.id.fragment_container, alarmsFragment, "alarms")
                listOf(homeFragment, scheduleFragment, alarmsFragment)
                    .filter { it != activeFragment }
                    .forEach { hide(it) }
            }.commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            scheduleFragment = supportFragmentManager.findFragmentByTag("schedule") as? ScheduleFragment ?: ScheduleFragment()
            alarmsFragment = supportFragmentManager.findFragmentByTag("alarms") as? AlarmsFragment ?: AlarmsFragment()
            activeFragment = listOf(homeFragment, scheduleFragment, alarmsFragment)
                .firstOrNull { !it.isHidden }
                ?: homeFragment
        }

        selectStartDestination(bottomNavigation, requestedDestination)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment)
                R.id.nav_schedule -> switchFragment(scheduleFragment)
                R.id.nav_create -> {
                    showQuickActions(bottomNavigation.findViewById(R.id.nav_create))
                    false
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, ChatBotActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun selectStartDestination(bottomNavigation: BottomNavigationView, destination: String) {
        bottomNavigation.selectedItemId = when (destination) {
            DESTINATION_SCHEDULE -> R.id.nav_schedule
            else -> R.id.nav_home
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
                    R.id.action_add_reminder -> {
                        startActivity(Intent(this@MainActivity, AddReminderActivity::class.java))
                        true
                    }
                    R.id.action_add_alarm -> {
                        startActivity(Intent(this@MainActivity, EditAlarmActivity::class.java))
                        true
                    }
                    R.id.action_alarms -> {
                        switchFragment(alarmsFragment)
                        true
                    }
                    R.id.action_search -> {
                        startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                        true
                    }
                    R.id.action_reports -> {
                        startActivity(Intent(this@MainActivity, ReportsActivity::class.java))
                        true
                    }
                    R.id.action_add_medicine -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Add Medicine feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_add_appointment -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Add Appointment feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_add_health_goal -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Add Health Goal feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_settings -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
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
