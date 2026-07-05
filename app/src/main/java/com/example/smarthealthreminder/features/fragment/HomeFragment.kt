package com.example.smarthealthreminder.features.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.smarthealthreminder.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.databinding.FragmentHomeDashboardBinding
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.adapter.WelcomeReminderAdapter
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.util.ImageUtils
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.example.smarthealthreminder.features.search.SearchActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var reminderAdapter: WelcomeReminderAdapter

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeReminders()
        setupProfileObservation()
        observeHealthStats()
    }

    private fun observeHealthStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todaySteps.collect { steps ->
                    steps?.let {
                        binding.tvDailySteps.text = String.format(Locale.getDefault(), "%,d", it.steps)
                    } ?: run {
                        binding.tvDailySteps.text = "0"
                    }
                }
            }
        }
    }

    private fun setupProfileObservation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUser.collect { user ->
                    user?.let {
                        binding.tvWelcomeName.text = getString(R.string.welcome_back_name, it.name)
                        it.profileImage?.let { base64 ->
                            val bitmap = ImageUtils.base64ToBitmap(base64)
                            binding.profileInfo.setImageBitmap(bitmap)
                        } ?: run {
                            binding.profileInfo.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
            }
        }
    }

    private fun refreshUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val localUser = DatabaseHelper(requireContext()).getUserByFirebaseId(uid)
                localUser?.let {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        viewModel.updateCurrentUser(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserProfile()
        updateDailyTipIfNeeded()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateDailyTipIfNeeded()
        }
    }

    private fun updateDailyTipIfNeeded() {
        val prefs = requireContext().getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_tip_date", "")
        val today = RecurrenceHelper.getTodayString()

        if (lastDate != today) {
            refreshDailyTip()
        } else {
            val savedTip = prefs.getString("current_tip", "")
            if (savedTip.isNullOrBlank()) {
                refreshDailyTip()
            } else {
                binding.tvTipContent.text = savedTip
            }
        }
    }

    private fun setupRecyclerView() {
        reminderAdapter = WelcomeReminderAdapter()
        binding.rvTodayReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayReminders.adapter = reminderAdapter

        reminderAdapter.setOnReminderClickListener { reminder ->
            Toast.makeText(context, "Clicked: ${reminder.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.searchIcon.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        binding.profileInfo.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
        binding.btnViewTodayPlan.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.smarthealthreminder.features.plan.TodayPlanActivity::class.java))
        }
        binding.calendarIcon.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SCHEDULE)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        binding.cvTodayReminders.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_REMINDERS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        binding.cvRemindingDoses.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_REMINDERS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        binding.btnRefreshTip.setOnClickListener {
            refreshDailyTip()
        }
        binding.cvDailySteps.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_INSIGHTS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

    }

    private fun refreshDailyTip() {
        val tips = listOf(
            "Drink at least 8 glasses of water today to stay hydrated.",
            "A 10-minute walk can significantly boost your mood and energy.",
            "Try to get 7-9 hours of sleep for optimal brain function.",
            "Taking deep breaths for 2 minutes can reduce stress levels.",
            "Include more leafy greens in your meals for essential vitamins.",
            "Consistency is key—keep up with your health reminders!",
            "Take a short break every hour to stretch your body.",
            "Practice mindfulness today: focus on the present moment.",
            "Replace sugary snacks with fruits for a natural energy boost.",
            "Regular exercise is a celebration of what your body can do.",
            "Don't be afraid to ask for help; mental health is just as important as physical health.",
            "Limit screen time an hour before bed for better sleep quality.",
            "Your value is not defined by your productivity.",
            "Small progress is still progress. Keep going!",
            "Start your day with a positive affirmation.",
            "Laughter is a great stress-reliever—watch something funny today.",
            "Social connection is vital; reach out to a friend or loved one.",
            "Listen to your body; if it needs rest, give it rest.",
            "Organize your workspace to clear your mind and reduce anxiety.",
            "Nature has a calming effect; spend some time outdoors if possible.",
            "Limit caffeine intake in the afternoon to avoid sleep disruption.",
            "Practice gratitude: name three things you're thankful for today.",
            "Avoid multi-tasking; focus on one thing at a time to reduce stress.",
            "Healthy eating isn't about restriction; it's about nourishment.",
            "Stretch for 5 minutes after waking up to improve circulation."
        )

        val currentTip = binding.tvTipContent.text.toString()
        var newTip = tips.random()
        
        // BUG FIX: Prevent infinite loop if tips list is small or same
        var attempts = 0
        while (newTip == currentTip && attempts < 10 && tips.size > 1) {
            newTip = tips.random()
            attempts++
        }
        
        binding.tvTipContent.text = newTip

        // Persist the tip and today's date
        val prefs = requireContext().getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("current_tip", newTip)
            putString("last_tip_date", RecurrenceHelper.getTodayString())
            apply()
        }
    }

    private fun observeReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReminders.collect { entities ->
                    val today = RecurrenceHelper.getTodayString()
                    
                    val totalTodayReminders = entities.filter { entity ->
                        RecurrenceHelper.isDueOnDate(
                            reminderDate = entity.date,
                            recurrenceType = entity.recurrenceType,
                            isRecurring = entity.isRecurring,
                            targetDate = today
                        )
                    }

                    val completedToday = totalTodayReminders.count { it.status.equals("Completed", ignoreCase = true) }
                    val totalCount = totalTodayReminders.size
                    
                    if (totalCount > 0) {
                        binding.reminding.text = getString(R.string.dosage_progress_format, completedToday, totalCount)
                    } else {
                        binding.reminding.text = "No doses today"
                    }

                    val todayReminders = totalTodayReminders
                        .filter { entity ->
                            entity.status.equals("Pending", ignoreCase = true) ||
                                    entity.status.equals("Snoozed", ignoreCase = true)
                        }
                        .sortedBy { it.time ?: "99:99" }

                    val reminders = todayReminders.map { entity ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
