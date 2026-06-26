package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.adapter.TimelineAdapter
import com.example.smarthealthreminder.features.model.TimelineItem
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RemindersFragment : Fragment() {

    private var recyclerTimeline: RecyclerView? = null
    private var timelineAdapter: TimelineAdapter? = null

    // Summary views
    private var tvTodayCount: TextView? = null
    private var tvMissedCount: TextView? = null
    private var tvCompletedCount: TextView? = null

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
        return inflater.inflate(R.layout.fragment_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTimeline = view.findViewById(R.id.recycler_timeline)
        recyclerTimeline?.layoutManager = LinearLayoutManager(context)

        timelineAdapter = TimelineAdapter()
        recyclerTimeline?.adapter = timelineAdapter

        // Summary views
        tvTodayCount = view.findViewById(R.id.tv_today_count)
        tvMissedCount = view.findViewById(R.id.tv_missed_count)
        tvCompletedCount = view.findViewById(R.id.tv_completed_count)

        timelineAdapter?.setOnItemClickListener(object : TimelineAdapter.OnItemClickListener {
            override fun onItemClick(item: TimelineItem) {
                // TODO: Navigate to reminder detail
            }
        })

        val btnAddReminder = view.findViewById<View>(R.id.btn_add_reminder)
        btnAddReminder?.setOnClickListener {
            startActivity(Intent(requireContext(), AddReminderActivity::class.java))
        }

        // Collect all reminders for timeline
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReminders.collect { reminders ->
                    val today = getTodayString()

                    val timelineItems = reminders.map { entity ->
                        // FIXED: Parse both old (MM/dd/yyyy) and new (yyyy-MM-dd) formats
                        val cal = parseDate(entity.date)

                        TimelineItem(
                            id = entity.id,
                            month = SimpleDateFormat("MMM", Locale.getDefault())
                                .format(cal.time).uppercase(),
                            day = cal.get(Calendar.DAY_OF_MONTH).toString(),
                            title = entity.title,
                            description = entity.description ?: "",
                            category = entity.category ?: "General",
                            time = entity.time ?: "--:--",
                            status = entity.status
                        )
                    }

                    timelineAdapter?.setItems(timelineItems)

                    // Update counts - FIXED: compare using normalized dates
                    val todayCount = reminders.count { normalizeDate(it.date) == today }
                    val missedCount = reminders.count { it.status == "Missed" }
                    val completedCount = reminders.count { it.status == "Completed" }

                    tvTodayCount?.text = todayCount.toString()
                    tvMissedCount?.text = String.format("%02d", missedCount)
                    tvCompletedCount?.text = String.format("%02d", completedCount)
                }
            }
        }
    }

    // FIXED: Parse date in either format
    private fun parseDate(dateStr: String?): Calendar {
        val cal = Calendar.getInstance()
        if (dateStr.isNullOrBlank()) return cal

        return try {
            // Try yyyy-MM-dd first (new format)
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf1.isLenient = false
            cal.time = sdf1.parse(dateStr) ?: Date()
            cal
        } catch (e: Exception) {
            try {
                // Fall back to MM/dd/yyyy (old format)
                val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                sdf2.isLenient = false
                cal.time = sdf2.parse(dateStr) ?: Date()
                cal
            } catch (e2: Exception) {
                cal
            }
        }
    }

    // FIXED: Normalize any date format to yyyy-MM-dd
    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val cal = parseDate(dateStr)
            String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH))
        } catch (e: Exception) {
            dateStr
        }
    }

    // FIXED: Return yyyy-MM-dd format
    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return String.format("%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH))
    }
}