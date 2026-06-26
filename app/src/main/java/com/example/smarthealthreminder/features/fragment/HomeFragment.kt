package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.databinding.FragmentHomeDashboardBinding
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.adapter.WelcomeReminderAdapter
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.search.SearchActivity
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

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
    }

    private fun setupRecyclerView() {
        reminderAdapter = WelcomeReminderAdapter()
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
    }

    private fun observeReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
