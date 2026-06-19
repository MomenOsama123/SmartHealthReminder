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
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.adapter.TimelineAdapter
import com.example.smarthealthreminder.features.model.TimelineItem
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
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
                        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                            isLenient = false
                        }
                        val date = runCatching { sdf.parse(entity.date.orEmpty()) }.getOrNull()
                        val cal = Calendar.getInstance().apply { time = date ?: Date() }

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

                    // Update counts
                    val todayCount = reminders.count { it.date == today }
                    val missedCount = reminders.count { it.status == "Missed" }
                    val completedCount = reminders.count { it.status == "Completed" }

                    tvTodayCount?.text = todayCount.toString()
                    tvMissedCount?.text = String.format("%02d", missedCount)
                    tvCompletedCount?.text = String.format("%02d", completedCount)
                }
            }
        }
    }

    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d/%02d/%04d",
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.YEAR))
    }
}
