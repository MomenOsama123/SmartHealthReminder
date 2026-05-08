package com.example.smarthealthreminder.features.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var currentReminder: Reminder? = null

    // Views
    private var tvTitle: TextView? = null
    private var tvSubtitle: TextView? = null
    private var tvTime: TextView? = null
    private var tvDate: TextView? = null
    private var tvPriority: TextView? = null
    private var tvDescription: TextView? = null
    private var emptyStateView: View? = null
    private var contentView: View? = null

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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvTitle = view.findViewById(R.id.tv_reminder_title)
        tvSubtitle = view.findViewById(R.id.tv_reminder_subtitle)
        tvTime = view.findViewById(R.id.tv_reminder_time)
        tvDate = view.findViewById(R.id.tv_reminder_date)
        tvPriority = view.findViewById(R.id.tv_reminder_priority)
        tvDescription = view.findViewById(R.id.tv_reminder_description)
        emptyStateView = view.findViewById(R.id.empty_state)
        contentView = view.findViewById(R.id.content_container)

        // Buttons
        val btnMarkDone = view.findViewById<View>(R.id.btn_mark_done)
        val btnEdit = view.findViewById<View>(R.id.btn_edit)
        val btnDelete = view.findViewById<View>(R.id.btn_delete)
        val btnAddReminder = view.findViewById<View>(R.id.btn_add_reminder)

        btnMarkDone?.setOnClickListener {
            currentReminder?.let { reminder ->
                reminder.id?.let { id ->
                    viewModel.markReminderDone(id)
                    Toast.makeText(context, "✅ Marked as done!", Toast.LENGTH_SHORT).show()
                }
            } ?: showNoReminderToast()
        }

        btnEdit?.setOnClickListener {
            currentReminder?.let { reminder ->
                // TODO: Open AddReminderActivity in edit mode
                Toast.makeText(context, "Edit: ${reminder.title}", Toast.LENGTH_SHORT).show()
            } ?: showNoReminderToast()
        }

        btnDelete?.setOnClickListener {
            currentReminder?.let { reminder ->
                showDeleteConfirmation(reminder)
            } ?: showNoReminderToast()
        }

        btnAddReminder?.setOnClickListener {
            startActivity(Intent(requireContext(), AddReminderActivity::class.java))
        }

        // Collect pending reminders - show first one as "current"
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingReminders.collect { reminders ->
                    if (reminders.isNotEmpty()) {
                        val entity = reminders.first()
                        currentReminder = Reminder(
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
                        showContent()
                        refreshUI()
                    } else {
                        currentReminder = null
                        showEmptyState()
                    }
                }
            }
        }
    }

    private fun showContent() {
        emptyStateView?.visibility = View.GONE
        contentView?.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        emptyStateView?.visibility = View.VISIBLE
        contentView?.visibility = View.GONE
    }

    private fun refreshUI() {
        val reminder = currentReminder
        if (reminder != null) {
            tvTitle?.text = reminder.title ?: "No Title"
            tvSubtitle?.text = if (reminder.isRecurring) 
                "Recurring • ${reminder.recurrenceType ?: ""}" 
                else "One-time reminder"
            tvTime?.text = reminder.time ?: "--:--"
            tvDate?.text = reminder.date ?: "--/--/----"
            tvPriority?.text = "! ${reminder.priority ?: "Normal"} Priority"
            tvDescription?.text = reminder.description ?: "No description available"
        }
    }

    private fun showDeleteConfirmation(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete \"${reminder.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                reminder.id?.let { id ->
                    viewModel.deleteReminderById(id)
                    Toast.makeText(context, "🗑️ Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNoReminderToast() {
        Toast.makeText(context, "No reminder selected", Toast.LENGTH_SHORT).show()
    }
}
