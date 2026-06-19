package com.example.smarthealthreminder.features.activity

import android.Manifest
import android.content.Context
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
import com.example.smarthealthreminder.features.search.SearchActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.fragment.AlarmsFragment
import com.example.smarthealthreminder.features.fragment.HomeFragment
import com.example.smarthealthreminder.features.fragment.ScheduleFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "smart_health_settings"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            scheduleFragment = ScheduleFragment()
            alarmsFragment = AlarmsFragment()
            activeFragment = homeFragment
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, scheduleFragment, "schedule").hide(scheduleFragment)
                .add(R.id.fragment_container, alarmsFragment, "alarms").hide(alarmsFragment)
                .commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            scheduleFragment = supportFragmentManager.findFragmentByTag("schedule") as? ScheduleFragment ?: ScheduleFragment()
            alarmsFragment = supportFragmentManager.findFragmentByTag("alarms") as? AlarmsFragment ?: AlarmsFragment()
            activeFragment = listOf(homeFragment, scheduleFragment, alarmsFragment).firstOrNull { !it.isHidden }
                ?: homeFragment
        }

        selectStartDestination(bottomNavigation, savedInstanceState == null)

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

    private fun selectStartDestination(bottomNavigation: BottomNavigationView, canSwitchImmediately: Boolean) {
        when (intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_HOME) {
            DESTINATION_SCHEDULE -> {
                bottomNavigation.selectedItemId = R.id.nav_schedule
                if (canSwitchImmediately) switchFragment(scheduleFragment)
            }
            DESTINATION_ALARMS -> {
                if (canSwitchImmediately) switchFragment(alarmsFragment)
            }
            else -> bottomNavigation.selectedItemId = R.id.nav_home
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
