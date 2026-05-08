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
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.adapter.AlarmAdapter
import com.example.smarthealthreminder.features.model.Alarm
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class AlarmsFragment : Fragment() {

    private var recyclerAlarms: RecyclerView? = null
    private var alarmAdapter: AlarmAdapter? = null
    private var emptyView: TextView? = null

    // Shared ViewModel across fragments
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
        return inflater.inflate(R.layout.fragment_alarms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerAlarms = view.findViewById(R.id.recycler_alarms)
        emptyView = view.findViewById(R.id.tv_empty)
        recyclerAlarms?.layoutManager = LinearLayoutManager(context)

        alarmAdapter = AlarmAdapter()
        recyclerAlarms?.adapter = alarmAdapter

        // Click on existing alarm to edit
        alarmAdapter?.setOnAlarmClickListener(object : AlarmAdapter.OnAlarmClickListener {
            override fun onAlarmClick(alarm: Alarm) {
                val intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarm.id)
                    putExtra("alarm_label", alarm.label)
                    putExtra("alarm_time", alarm.time)
                    putExtra("alarm_am_pm", alarm.amPm)
                    putExtra("alarm_category", alarm.category)
                    putExtra("alarm_repeat_days", alarm.repeatDays)
                }
                startActivity(intent)
            }
        })

        // Toggle alarm on/off
        alarmAdapter?.setOnAlarmToggleListener(object : AlarmAdapter.OnAlarmToggleListener {
            override fun onToggle(alarm: Alarm, isActive: Boolean) {
                alarm.id?.let { id ->
                    viewModel.toggleAlarm(id, isActive)
                }
            }
        })

        // Add Alarm Button
        val btnAdd = view.findViewById<View>(R.id.btn_add)
        btnAdd?.setOnClickListener {
            val intent = Intent(requireContext(), EditAlarmActivity::class.java)
            startActivity(intent)
        }

        // Collect alarms from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allAlarms.collect { alarmEntities ->
                    val alarms = alarmEntities.map { entity ->
                        Alarm(
                            id = entity.id,
                            label = entity.label,
                            time = entity.time,
                            amPm = entity.amPm,
                            category = entity.category,
                            isActive = entity.isActive,
                            repeatDays = entity.repeatDays,
                            sound = entity.sound,
                            vibrationEnabled = entity.vibrationEnabled,
                            gradualVolume = entity.gradualVolume,
                            autoSnoozeMinutes = entity.autoSnoozeMinutes,
                            cognitiveLockEnabled = entity.cognitiveLockEnabled
                        )
                    }
                    alarmAdapter?.setAlarms(alarms)
                    checkEmptyState()
                }
            }
        }
    }

    private fun checkEmptyState() {
        if (alarmAdapter?.itemCount == 0) {
            recyclerAlarms?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        } else {
            recyclerAlarms?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        }
    }
}
