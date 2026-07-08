package com.example.smarthealthreminder.features.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotFragment
import com.example.smarthealthreminder.features.dialog.QuickActionsBottomSheet
import com.example.smarthealthreminder.features.fragment.AddReminderFragment
import com.example.smarthealthreminder.features.fragment.AlarmsFragment
import com.example.smarthealthreminder.features.fragment.DashboardFragment
import com.example.smarthealthreminder.features.fragment.HomeFragment
import com.example.smarthealthreminder.features.fragment.RemindersFragment
import com.example.smarthealthreminder.features.fragment.ScheduleFragment
import com.example.smarthealthreminder.features.settings.SettingsFragment
import com.example.smarthealthreminder.features.stepsTracker.StepsTrackerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.smarthealthreminder.features.fragment.MedicationPlansFragment

import com.example.smarthealthreminder.features.reports.ReportsFragment

class MainActivity : BaseActivity() {

    companion object {

        const val DESTINATION_MEDICATION_PLANS = "medication_plans"

        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_ALARMS = "alarms"
        const val DESTINATION_REMINDERS = "reminders"
        const val DESTINATION_SETTINGS = "settings"
        const val DESTINATION_INSIGHTS = "insights"

        // 🌟 1. NEW: Added a constant for the reports destination
        const val DESTINATION_REPORTS = "reports"
        const val DESTINATION_DASHBOARD = "dashboard"
        const val DESTINATION_ADD_REMINDER = "add_reminder"

        private const val REQUEST_POST_NOTIFICATIONS = 2001
        private const val TAG_HOME = "home"
        private const val TAG_SCHEDULE = "schedule"
        private const val TAG_ALARMS = "alarms"
        private const val TAG_REMINDERS = "reminders"
        private const val TAG_SETTINGS = "settings"
        private const val TAG_INSIGHTS = "insights"
        private const val TAG_MEDICATION_PLANS = "medication_plans"
        private const val TAG_REPORTS = "reports"
        private const val TAG_DASHBOARD = "dashboard"
        private const val TAG_ADD_REMINDER = "add_reminder"
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var alarmsFragment: AlarmsFragment
    private lateinit var remindersFragment: RemindersFragment
    private lateinit var chatBotFragment: ChatBotFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var stepsTrackerFragment: StepsTrackerFragment
    private lateinit var reportsFragment: ReportsFragment
    private lateinit var dashboardFragment: DashboardFragment
    private lateinit var addReminderFragment: AddReminderFragment

    private lateinit var bottomNavigation: BottomNavigationView
    private var activeFragment: Fragment? = null
    private var isProgrammaticSelection = false

    private lateinit var medicationPlansFragment: MedicationPlansFragment



    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
            medicationPlansFragment = MedicationPlansFragment()
            reportsFragment = ReportsFragment()
            dashboardFragment = DashboardFragment()
            addReminderFragment = AddReminderFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, scheduleFragment, TAG_SCHEDULE).hide(scheduleFragment)
                .add(R.id.fragment_container, alarmsFragment, TAG_ALARMS).hide(alarmsFragment)
                .add(R.id.fragment_container, remindersFragment, TAG_REMINDERS).hide(remindersFragment)
                .add(R.id.fragment_container, chatBotFragment, "chatbot").hide(chatBotFragment)
                .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                .add(R.id.fragment_container, stepsTrackerFragment, TAG_INSIGHTS).hide(stepsTrackerFragment)
                .add(R.id.fragment_container, reportsFragment, TAG_REPORTS).hide(reportsFragment)
                .add(R.id.fragment_container, dashboardFragment, TAG_DASHBOARD).hide(dashboardFragment)
                .add(R.id.fragment_container, addReminderFragment, TAG_ADD_REMINDER).hide(addReminderFragment)
                .add(R.id.fragment_container, homeFragment, TAG_HOME)

                .add(R.id.fragment_container, medicationPlansFragment, TAG_MEDICATION_PLANS).hide(medicationPlansFragment)

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
            reportsFragment = supportFragmentManager.findFragmentByTag(TAG_REPORTS) as? ReportsFragment ?: ReportsFragment()
            dashboardFragment = supportFragmentManager.findFragmentByTag(TAG_DASHBOARD) as? DashboardFragment ?: DashboardFragment()
            addReminderFragment = supportFragmentManager.findFragmentByTag(TAG_ADD_REMINDER) as? AddReminderFragment ?: AddReminderFragment()

            activeFragment = supportFragmentManager.fragments.find { it.isAdded && !it.isHidden } ?: homeFragment
            medicationPlansFragment = supportFragmentManager.findFragmentByTag(TAG_MEDICATION_PLANS) as? MedicationPlansFragment ?: MedicationPlansFragment()

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

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else if (activeFragment != homeFragment) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Handle Keyboard visibility and System Bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())

            v.visibility = if (imeVisible) android.view.View.GONE else android.view.View.VISIBLE

            // Adjust margin to stay above navigation bar
            val params = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBars.bottom + (16 * resources.displayMetrics.density).toInt()
            v.layoutParams = params

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
            DESTINATION_MEDICATION_PLANS -> {
                showFragment(medicationPlansFragment)}
            // 🌟 7. NEW: Navigate to the reports fragment when requested by the intent
            DESTINATION_REPORTS -> {
                showFragment(reportsFragment)
            }

            DESTINATION_DASHBOARD -> {
                showFragment(dashboardFragment)
            }
            DESTINATION_ADD_REMINDER -> {
                showFragment(addReminderFragment)
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

    fun openAddMedicationPlanFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, com.example.smarthealthreminder.features.fragment.AddMedicationPlanFragment(), "add_medication_plan")
            .addToBackStack("add_medication_plan")
            .commit()
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
