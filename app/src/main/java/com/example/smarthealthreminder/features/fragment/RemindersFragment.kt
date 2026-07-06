package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.smarthealthreminder.features.activity.MainActivity
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
    private var tvViewCalendar: TextView? = null
    private var cardToday: View? = null
    private var cardMissed: View? = null
    private var cardCompleted: View? = null

    // Filter state
    private var currentFilter: String? = null
    private var latestTimelineItems = listOf<TimelineItem>()

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
        tvViewCalendar = view.findViewById(R.id.tv_view_calendar)
        cardToday = view.findViewById(R.id.card_today)
        cardMissed = view.findViewById(R.id.card_missed)
        cardCompleted = view.findViewById(R.id.card_completed)

        setupTimelineActions()
        setupCardClickListeners()
        setupViewCalendar()

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
                        val cal = parseDate(entity.date)

                        TimelineItem(
                            id = entity.id,
                            month = SimpleDateFormat("MMM", Locale.getDefault())
                                .format(cal.time).uppercase(),
                            day = cal.get(Calendar.DAY_OF_MONTH).toString(),
                            date = entity.date,
                            title = entity.title,
                            description = entity.description ?: "",
                            category = entity.category ?: "General",
                            time = entity.time ?: "--:--",
                            status = entity.status
                        )
                    }

                    latestTimelineItems = timelineItems
                    applyFilter(timelineItems)

                    // Update counts
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

    private fun setupTimelineActions() {
        timelineAdapter?.setOnItemActionListener(object : TimelineAdapter.OnItemActionListener {
            override fun onItemClick(item: TimelineItem) {
                // Item click is handled by the dialog in the adapter
            }

            override fun onMarkDone(item: TimelineItem) {
                item.id?.let { id ->
                    viewModel.markReminderDone(id)
                    Toast.makeText(requireContext(), "Marked as done", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMarkMissed(item: TimelineItem) {
                item.id?.let { id ->
                    viewModel.markReminderMissed(id)
                    Toast.makeText(requireContext(), "Marked as missed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDelete(item: TimelineItem) {
                item.id?.let { id ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Reminder")
                        .setMessage("Are you sure you want to delete this reminder?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteReminderById(id)
                            Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        })
    }

    private fun setupCardClickListeners() {
        cardToday?.setOnClickListener {
            currentFilter = null
            applyFilterToCurrentItems()
            highlightCard(cardToday)
        }

        cardMissed?.setOnClickListener {
            currentFilter = "Missed"
            applyFilterToCurrentItems()
            highlightCard(cardMissed)
        }

        cardCompleted?.setOnClickListener {
            currentFilter = "Completed"
            applyFilterToCurrentItems()
            highlightCard(cardCompleted)
        }
    }

    private fun setupViewCalendar() {
        tvViewCalendar?.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SCHEDULE)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    private fun highlightCard(selected: View?) {
        // Reset all cards to default elevation
        cardToday?.elevation = 0f
        cardMissed?.elevation = 0f
        cardCompleted?.elevation = 0f

        // Add a subtle elevation to the selected card
        selected?.elevation = 8f
    }

    private fun applyFilterToCurrentItems() {
        applyFilter(latestTimelineItems)
    }

    private fun applyFilter(items: List<TimelineItem>) {
        val filtered = when (currentFilter) {
            "Missed" -> items.filter { it.status.equals("Missed", ignoreCase = true) }
            "Completed" -> items.filter {
                it.status.equals("Completed", ignoreCase = true) || it.status.equals("Done", ignoreCase = true)
            }
            else -> items
        }
        timelineAdapter?.setItems(filtered)
    }

    // FIXED: Parse date in either format
    private fun parseDate(dateStr: String?): Calendar {
        val cal = Calendar.getInstance()
        if (dateStr.isNullOrBlank()) return cal

        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf1.isLenient = false
            cal.time = sdf1.parse(dateStr) ?: Date()
            cal
        } catch (e: Exception) {
            try {
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
