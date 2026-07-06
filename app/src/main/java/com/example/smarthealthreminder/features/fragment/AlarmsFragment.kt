package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.adapter.AlarmAdapter
import com.example.smarthealthreminder.features.model.Alarm
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
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

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        recyclerAlarms = view.findViewById(R.id.recycler_alarms)
        emptyView = view.findViewById(R.id.tv_empty)
        recyclerAlarms?.layoutManager = LinearLayoutManager(context)

        alarmAdapter = AlarmAdapter()
        recyclerAlarms?.adapter = alarmAdapter

        // Click on existing alarm to edit
        alarmAdapter?.setOnAlarmClickListener(object : AlarmAdapter.OnAlarmClickListener {
            override fun onAlarmClick(alarm: Alarm) {
                val intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
                    putExtra(EditAlarmActivity.EXTRA_ALARM_ID, alarm.id)
                }
                startActivity(intent)
            }
        })

        // Toggle alarm on/off
        alarmAdapter?.setOnAlarmToggleListener(object : AlarmAdapter.OnAlarmToggleListener {
            override fun onToggle(alarm: Alarm, isActive: Boolean) {
                alarm.id?.let { id ->
                    val alarmHelper = com.example.smarthealthreminder.alarm.AlarmHelper(requireContext())
                    if (isActive) {
                        if (!alarmHelper.canScheduleExactAlarm()) {
                            alarm.isActive = false
                            alarmAdapter?.updateAlarm(alarm)
                            Toast.makeText(
                                requireContext(),
                                "Allow exact alarms so this alarm can ring on time",
                                Toast.LENGTH_LONG
                            ).show()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${requireContext().packageName}")
                                })
                            }
                            return
                        }
                        val scheduled = alarmHelper.scheduleAlarm(alarm)
                        if (scheduled) {
                            viewModel.toggleAlarm(id, true)
                        } else {
                            alarm.isActive = false
                            alarmAdapter?.updateAlarm(alarm)
                            viewModel.toggleAlarm(id, false)
                            Toast.makeText(
                                requireContext(),
                                "Could not schedule alarm. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        alarmHelper.cancelAlarm(alarm)
                        viewModel.toggleAlarm(id, false)
                    }
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
