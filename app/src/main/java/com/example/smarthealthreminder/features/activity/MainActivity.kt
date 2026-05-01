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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.nav_health -> {
                    loadFragment(AlarmsFragment())
                    true
                }
                R.id.nav_settings -> {
                    // TODO: loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}