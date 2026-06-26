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
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.databinding.ActivityMainWelcomeBinding
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.adapter.WelcomeReminderAdapter
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.search.SearchActivity
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class MainWelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainWelcomeBinding
    private lateinit var reminderAdapter: WelcomeReminderAdapter

    private val viewModel: HealthViewModel by viewModels {
        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        setupWindowInsets()
        observeReminders()
    }

    private fun setupRecyclerView() {
        reminderAdapter = WelcomeReminderAdapter()
        binding.rvTodayReminders.adapter = reminderAdapter

        reminderAdapter.setOnReminderClickListener { reminder ->
            Toast.makeText(this, "Clicked: ${reminder.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeReminders() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingReminders.collect { entities ->
                    val reminders = entities.map { entity ->
                        Reminder(
                            id = entity.id,
                            title = entity.title,
                            description = entity.description,
                            category = entity.category,
                            date = entity.date,
                            time = entity.time,
                            priority = entity.priority,
                            status = entity.status,
                            isRecurring = entity.isRecurring,
                            recurrenceType = entity.recurrenceType,
                            vibrationEnabled = entity.vibrationEnabled,
                            earlyNotification = entity.earlyNotification,
                            earlyNotificationMinutes = entity.earlyNotificationMinutes
                        )
                    }
                    reminderAdapter.setReminders(reminders)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.searchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }
        binding.profileInfo.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.btnViewTodayPlan.setOnClickListener {
            startActivity(Intent(this, com.example.smarthealthreminder.features.plan.TodayPlanActivity::class.java))
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
                    QuickActionsBottomSheet().show(
                        supportFragmentManager,
                        QuickActionsBottomSheet.TAG
                    )
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
                    R.id.reminder -> {
                        startActivity(Intent(this@MainWelcomeActivity, AddReminderActivity::class.java))
                        true
                    }
                    R.id.action_alarms -> {
                        openMainSection(MainActivity.DESTINATION_ALARMS)
                        true
                    }
                    R.id.action_reports -> {
                        startActivity(Intent(this@MainWelcomeActivity, ReportsActivity::class.java))
                        true
                    }
                    else -> true
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