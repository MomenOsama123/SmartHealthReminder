package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.adapter.MedicationPlansListAdapter
import com.example.smarthealthreminder.features.alarm.ReminderScheduler
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.dialog.MedicationPlanDetailDialogHelper
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class MedicationPlansFragment : Fragment() {

    private var recyclerPlans: RecyclerView? = null
    private var plansAdapter: MedicationPlansListAdapter? = null
    private var emptyView: TextView? = null

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_medication_plans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerPlans = view.findViewById(R.id.recycler_medication_plans)
        emptyView = view.findViewById(R.id.tv_empty_plans)
        recyclerPlans?.layoutManager = LinearLayoutManager(context)

        plansAdapter = MedicationPlansListAdapter()
        recyclerPlans?.adapter = plansAdapter

        plansAdapter?.setOnPlanClickListener { plan ->
            MedicationPlanDetailDialogHelper.show(
                context = requireContext(),
                plan = plan,
                onStopPlan = { stopMedicationPlan(plan) }
            )
        }

        view.findViewById<ImageButton>(R.id.btn_add)?.setOnClickListener {
            (requireActivity() as MainActivity).openAddMedicationPlanFragment()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allMedicationPlans.collect { plans ->
                    if (plans.isEmpty()) {
                        recyclerPlans?.visibility = View.GONE
                        emptyView?.visibility = View.VISIBLE
                    } else {
                        recyclerPlans?.visibility = View.VISIBLE
                        emptyView?.visibility = View.GONE
                        plansAdapter?.submitList(plans.sortedByDescending { it.isActive })
                    }
                }
            }
        }
    }

    private fun stopMedicationPlan(plan: MedicationPlanEntity) {
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
}