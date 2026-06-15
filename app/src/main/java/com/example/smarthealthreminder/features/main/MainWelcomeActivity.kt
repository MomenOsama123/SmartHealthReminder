package com.example.smarthealthreminder.features.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.ActivityMainWelcomeBinding
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.features.Search.SearchActivity
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.settings.SettingsActivity

class MainWelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupBottomNavigation()
        setupWindowInsets()
    }

    private fun setupClickListeners() {
        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }
        binding.profileInfo.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_schedule -> {
                    openMainSection(MainActivity.DESTINATION_SCHEDULE)
                    false
                }
                R.id.nav_create -> {
                    showQuickActions(binding.bottomNavigation.findViewById(R.id.nav_create))
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

    private fun openMainSection(destination: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, destination)
        })
    }

    private fun showQuickActions(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.quick_actions_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_reminder -> {
                        startActivity(Intent(this@MainWelcomeActivity, AddReminderActivity::class.java))
                        true
                    }
                    R.id.action_add_alarm -> {
                        startActivity(Intent(this@MainWelcomeActivity, EditAlarmActivity::class.java))
                        true
                    }
                    R.id.action_alarms -> {
                        openMainSection(MainActivity.DESTINATION_ALARMS)
                        true
                    }
                    R.id.action_search -> {
                        startActivity(Intent(this@MainWelcomeActivity, SearchActivity::class.java))
                        true
                    }
                    R.id.action_reports -> {
                        startActivity(Intent(this@MainWelcomeActivity, ReportsActivity::class.java))
                        true
                    }
                    R.id.action_add_medicine -> {
                        Toast.makeText(
                            this@MainWelcomeActivity,
                            "Add Medicine feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_add_appointment -> {
                        Toast.makeText(
                            this@MainWelcomeActivity,
                            "Add Appointment feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_add_health_goal -> {
                        Toast.makeText(
                            this@MainWelcomeActivity,
                            "Add Health Goal feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.action_settings -> {
                        startActivity(Intent(this@MainWelcomeActivity, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
