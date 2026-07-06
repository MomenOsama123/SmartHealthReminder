package com.example.smarthealthreminder.features.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class ViewReminderFragment : Fragment() {

    private var reminderId: String? = null

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reminderId = it.getString(ARG_REMINDER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_view_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
        loadReminderData(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.finish()
        }

        view.findViewById<View>(R.id.btn_edit_bottom).setOnClickListener {
            navigateToEdit()
        }

        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            activity?.finish()
        }
    }

    private fun navigateToEdit() {
        reminderId?.let { id ->
            val intent = Intent(requireContext(), AddReminderActivity::class.java).apply {
                putExtra(AddReminderActivity.EXTRA_REMINDER_ID, id)
            }
            startActivity(intent)
        }
    }

    private fun loadReminderData(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_reminder_title)
        val tvDescription = view.findViewById<TextView>(R.id.tv_reminder_description)
        val tvTime = view.findViewById<TextView>(R.id.tv_reminder_time)
        val tvRecurrenceSummary = view.findViewById<TextView>(R.id.tv_recurrence_summary)
        val tvRecurrenceType = view.findViewById<TextView>(R.id.tv_recurrence_type)
        val tvPriorityValue = view.findViewById<TextView>(R.id.tv_priority_value)
        val tvEarlyNotificationValue = view.findViewById<TextView>(R.id.tv_early_notification_value)
        val tvVibrationValue = view.findViewById<TextView>(R.id.tv_vibration_value)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_reminder_category_icon)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tv_status_badge)
        val tvStatusValue = view.findViewById<TextView>(R.id.tv_status_value)
        val ivStatusIcon = view.findViewById<ImageView>(R.id.iv_status_icon)

        reminderId?.let { id ->
            viewLifecycleOwner.lifecycleScope.launch {
                val reminder = viewModel.repository.getReminderById(id)
                reminder?.let {
                    tvTitle.text = it.title
                    tvDescription.text = it.description ?: "No description provided."
                    tvTime.text = it.time
                    tvRecurrenceSummary.text = it.recurrenceType ?: "One-time"
                    tvRecurrenceType.text = it.recurrenceType ?: "Once"
                    tvPriorityValue.text = it.priority ?: "Normal"
                    tvEarlyNotificationValue.text = if (it.earlyNotification) "${it.earlyNotificationMinutes} minutes before" else "None"
                    tvVibrationValue.text = if (it.vibrationEnabled) "Enabled" else "Disabled"
                    
                    // Set category icon
                    when (it.category?.lowercase()) {
                        "medicine", "meds" -> ivIcon.setImageResource(R.drawable.ic_medicine)
                        "appointment" -> ivIcon.setImageResource(R.drawable.ic_appointment)
                        "task" -> ivIcon.setImageResource(R.drawable.ic_task)
                        else -> ivIcon.setImageResource(R.drawable.ic_medicine)
                    }
                    
                    // Update status badge and row
                    when (it.status) {
                        "Taken", "Completed" -> {
                            val statusText = getString(R.string.marked_done)
                            tvStatusBadge.text = statusText
                            tvStatusValue.text = statusText
                            
                            tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.success_light)
                            )
                            tvStatusBadge.setTextColor(requireContext().getColor(R.color.success))
                            ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.success)
                            )
                        }
                        "Missed" -> {
                            val statusText = getString(R.string.missed)
                            tvStatusBadge.text = statusText
                            tvStatusValue.text = statusText
                            
                            tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.urgent_light)
                            )
                            tvStatusBadge.setTextColor(requireContext().getColor(R.color.urgent))
                            ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.urgent)
                            )
                        }
                        "Snoozed" -> {
                            val statusText = getString(R.string.snoozed)
                            tvStatusBadge.text = statusText
                            tvStatusValue.text = statusText
                            
                            tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.pending_light)
                            )
                            tvStatusBadge.setTextColor(requireContext().getColor(R.color.pending))
                            ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.pending)
                            )
                        }
                        else -> { // Pending / Active
                            val statusText = getString(R.string.active_reminder)
                            tvStatusBadge.text = statusText
                            tvStatusValue.text = statusText
                            
                            tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.success_light)
                            )
                            tvStatusBadge.setTextColor(requireContext().getColor(R.color.success))
                            ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.success)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_REMINDER_ID = "reminder_id"

        @JvmStatic
        fun newInstance(reminderId: String) =
            ViewReminderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REMINDER_ID, reminderId)
                }
            }
    }
}
