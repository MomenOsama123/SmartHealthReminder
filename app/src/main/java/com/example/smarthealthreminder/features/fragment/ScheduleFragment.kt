package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.adapter.TimelineAdapter
import com.example.smarthealthreminder.features.model.TimelineItem

class ScheduleFragment : Fragment() {

    private var recyclerTimeline: RecyclerView? = null
    private var timelineAdapter: TimelineAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTimeline = view.findViewById(R.id.recycler_timeline)
        recyclerTimeline?.layoutManager = LinearLayoutManager(context)

        timelineAdapter = TimelineAdapter()
        recyclerTimeline?.adapter = timelineAdapter

        // Load mock data
        loadMockData()

        val btnAddReminder = view.findViewById<View>(R.id.btn_add_reminder)
        btnAddReminder?.setOnClickListener {
            // Navigate to add reminder
        }
    }

    private fun loadMockData() {
        val items = listOf(
            TimelineItem("OCT", "24", "Morning Insulin Dose",
                "Take 15 units before breakfast...", "Medicine", "08:00 AM", "MISSED"),
            TimelineItem("OCT", "24", "Physiotherapy Session",
                "Lower back strengthening...", "Health", "02:30 PM", "PENDING"),
            TimelineItem("OCT", "23", "Vitamin D Supplement",
                "Daily capsule with dinner.", "Personal", "07:00 PM", "DONE"),
            TimelineItem("OCT", "25", "Health Insurance Renewal",
                "Review policy updates for next...", "Work", "10:00 AM", "PENDING")
        )

        timelineAdapter?.setItems(items)
    }
}