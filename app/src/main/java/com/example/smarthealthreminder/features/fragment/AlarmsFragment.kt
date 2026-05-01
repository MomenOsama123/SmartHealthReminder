package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.adapter.AlarmAdapter
import com.example.smarthealthreminder.features.model.Alarm

class AlarmsFragment : Fragment() {

    private var recyclerAlarms: RecyclerView? = null
    private var alarmAdapter: AlarmAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerAlarms = view.findViewById(R.id.recycler_alarms)
        recyclerAlarms?.layoutManager = LinearLayoutManager(context)

        alarmAdapter = AlarmAdapter()
        recyclerAlarms?.adapter = alarmAdapter

        loadMockData()
    }

    private fun loadMockData() {
        val alarms = listOf(
            Alarm("Morning Medication", "08:00", "AM", "MEDICINE"),
            Alarm("Check-up", "12:30", "PM", "CHECK-UP"),
            Alarm("Evening Dose", "22:00", "PM", "MEDICINE")
        )

        alarms[0].repeatDays = "M T W T F S S"
        alarms[1].repeatDays = "M W T F S S"
        alarms[2].repeatDays = "M T W T F S S"

        alarmAdapter?.setAlarms(alarms)
    }
}