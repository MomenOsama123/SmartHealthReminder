package com.example.smarthealthreminder.features.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.smarthealthreminder.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.databinding.FragmentHomeDashboardBinding
import com.example.smarthealthreminder.features.adapter.WelcomeReminderAdapter
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.util.ImageUtils
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.example.smarthealthreminder.features.search.SearchActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.example.smarthealthreminder.features.ui_dashboard.DashboardActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthealthreminder.features.profileInfo.ProfileActivity
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

import com.example.smarthealthreminder.features.adapter.MedicationPlanAdapter
import com.example.smarthealthreminder.features.dialog.ReminderDetailDialogHelper
import com.example.smarthealthreminder.features.dialog.MedicationPlanDetailDialogHelper
import com.example.smarthealthreminder.features.alarm.ReminderScheduler
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var medicationPlanAdapter: MedicationPlanAdapter
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

        setupMedicationPlansSection()
        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupRecyclerView()
        setupClickListeners()
        observeReminders()
        setupProfileObservation()
        observeHealthStats()
        observeAdherence()
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
        binding.rvTodayReminders.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        binding.rvTodayReminders.adapter = reminderAdapter

        reminderAdapter.setOnReminderClickListener { reminder ->
            val intent = Intent(requireContext(), com.example.smarthealthreminder.features.activity.ViewReminderActivity::class.java).apply {
                putExtra(com.example.smarthealthreminder.features.activity.ViewReminderActivity.EXTRA_REMINDER_ID, reminder.id)
            }
            startActivity(intent)
        }

        reminderAdapter.setOnStatusClickListener { reminder ->
            if (reminder.status != "Completed") {
                reminder.id?.let {
                    viewModel.markReminderDone(it)
                    Toast.makeText(requireContext(), "Reminder marked as done!", Toast.LENGTH_SHORT).show()
                }
            }
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
        binding.cvDashboard.setOnClickListener {
            (activity as? MainActivity)?.navigateToDestination(MainActivity.DESTINATION_DASHBOARD)
        }
    }

    private fun refreshDailyTip() {
        val tips = resources.getStringArray(R.array.health_tips)
        if (tips.isEmpty()) return

        // We use the cached tip from preferences to check for duplicates, 
        // not the current TextView text which might be in a different language or empty.
        val prefs = requireContext().getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
        val cachedTip = prefs.getString("current_tip", "")
        
        var newTip = tips.random()

        var attempts = 0
        // Try to get a different tip than what was previously shown (if any)
        while (newTip == cachedTip && attempts < 10 && tips.size > 1) {
            newTip = tips.random()
            attempts++
        }

        binding.tvTipContent.text = newTip

        // Persist the tip and today's date
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
                        binding.reminding.text = getString(R.string.no_doses_today)
                    }

                    val todayReminders = totalTodayReminders
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

    private fun setupMedicationPlansSection() {
        medicationPlanAdapter = MedicationPlanAdapter()
        binding.rvMedicationPlans.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMedicationPlans.adapter = medicationPlanAdapter
        medicationPlanAdapter.setOnPlanClickListener { plan ->
            MedicationPlanDetailDialogHelper.show(
                context = requireContext(),
                plan = plan,
                onStopPlan = { stopMedicationPlan(plan) }
            )
        }

        binding.btnAddMedicationPlanHome.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_MEDICATION_PLANS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allMedicationPlans.collect { plans ->
                    val today = RecurrenceHelper.getTodayString()
                    val activePlans = plans.filter { it.isActive && it.endDate >= today }

                    if (activePlans.isEmpty()) {
                        binding.rvMedicationPlans.visibility = View.GONE
                        binding.tvNoMedicationPlans.visibility = View.VISIBLE
                    } else {
                        binding.rvMedicationPlans.visibility = View.VISIBLE
                        binding.tvNoMedicationPlans.visibility = View.GONE
                        medicationPlanAdapter.submitList(activePlans)
                    }
                }
            }
        }
    }

    private fun observeAdherence() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReminders.collect { reminders ->

                    val today = RecurrenceHelper.getTodayString()

                    val todayReminders = reminders.filter {
                        RecurrenceHelper.isDueOnDate(
                            reminderDate = it.date,
                            recurrenceType = it.recurrenceType,
                            isRecurring = it.isRecurring,
                            targetDate = today
                        )
                    }

                    val total = todayReminders.size
                    val completed = todayReminders.count {
                        it.status.equals("Completed", true)
                    }

                    val percent =
                        if (total > 0) (completed * 100) / total else 0

                    binding.tvHomeAdherencePercent.text = "$percent%"
                    binding.pbHomeAdherence.progress = percent
                }
            }
        }
    }

    private fun stopMedicationPlan(plan: com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity) {
        val repository = HealthRepository(AppDatabase.getDatabase(requireContext()))
        lifecycleScope.launch {
            val reminders = repository.getRemindersByPlanId(plan.id)
            reminders.forEach { reminder ->
                ReminderScheduler.cancelReminderAlarms(requireContext(), reminder.id)
                repository.updateReminderStatus(reminder.id, "Completed")
            }
            repository.deactivateMedicationPlan(plan.id)
            Toast.makeText(requireContext(), "\"${plan.medicineName}\" plan stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}