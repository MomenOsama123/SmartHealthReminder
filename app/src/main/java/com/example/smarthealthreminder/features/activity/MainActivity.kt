package com.example.smarthealthreminder.features.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.fragment.AlarmsFragment
import com.example.smarthealthreminder.features.fragment.HomeFragment
import com.example.smarthealthreminder.features.fragment.ScheduleFragment

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Initialize fragments
        homeFragment = HomeFragment()
        scheduleFragment = ScheduleFragment()
        alarmsFragment = AlarmsFragment()

        // Set default fragment
        if (savedInstanceState == null) {
            activeFragment = homeFragment
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, scheduleFragment, "schedule").hide(scheduleFragment)
                .add(R.id.fragment_container, alarmsFragment, "alarms").hide(alarmsFragment)
                .commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment)
                R.id.nav_schedule -> switchFragment(scheduleFragment)
                R.id.nav_health -> switchFragment(alarmsFragment)
                R.id.nav_settings -> {
                    // TODO: loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment): Boolean {
        if (activeFragment == fragment) return true

        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragment)
            .commit()

        activeFragment = fragment
        return true
    }
}
