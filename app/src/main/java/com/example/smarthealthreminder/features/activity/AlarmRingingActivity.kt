package com.example.smarthealthreminder.features.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmRingingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_TIME = "alarm_time"
        const val EXTRA_ALARM_CATEGORY = "alarm_category"
        const val EXTRA_ALARM_PRIORITY = "alarm_priority"
        const val EXTRA_PATIENT_NAME = "patient_name"
        const val EXTRA_ROOM_INFO = "room_info"
    }

    private var alarmId: String? = null
    private lateinit var alarmHelper: AlarmHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_ringing)

        alarmHelper = AlarmHelper(this)

        // Get data from intent
        alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val time = intent.getStringExtra(EXTRA_ALARM_TIME) ?: getCurrentTime()
        val category = intent.getStringExtra(EXTRA_ALARM_CATEGORY) ?: "General"
        val priority = intent.getStringExtra(EXTRA_ALARM_PRIORITY) ?: "NORMAL"
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: ""
        val roomInfo = intent.getStringExtra(EXTRA_ROOM_INFO) ?: ""

        // Bind dynamic data to views
        val tvTime = findViewById<TextView>(R.id.tv_alarm_time)
        val tvDate = findViewById<TextView>(R.id.tv_alarm_date)
        val tvLabel = findViewById<TextView>(R.id.tv_alarm_label)
        val tvCategory = findViewById<TextView>(R.id.tv_alarm_category)
        val tvPriority = findViewById<TextView>(R.id.tv_alarm_priority)
        val tvPatientName = findViewById<TextView>(R.id.tv_patient_name)
        val tvRoomInfo = findViewById<TextView>(R.id.tv_room_info)

        tvTime?.text = time
        tvDate?.text = getCurrentDate()
        tvLabel?.text = label
        tvCategory?.text = category
        tvPriority?.text = priority
        tvPatientName?.text = if (patientName.isNotEmpty()) "Patient: $patientName" else ""
        tvRoomInfo?.text = roomInfo

        val btnSnooze = findViewById<Button>(R.id.btn_snooze)
        val btnStop = findViewById<Button>(R.id.btn_stop_alarm)

        btnSnooze?.setOnClickListener {
            // Snooze for 10 minutes
            lifecycleScope.launch {
                // TODO: Get alarm from database and snooze
                // For now, just finish
                finish()
            }
        }

        btnStop?.setOnClickListener {
            // Stop alarm
            stopService(Intent(this, com.example.smarthealthreminder.alarm.AlarmService::class.java))
            finish()
        }

        // Start alarm service
        startService(Intent(this, com.example.smarthealthreminder.alarm.AlarmService::class.java).apply {
            putExtra(com.example.smarthealthreminder.alarm.AlarmService.EXTRA_ALARM_LABEL, label)
        })
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        return sdf.format(calendar.time).uppercase()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure service is stopped
        stopService(Intent(this, com.example.smarthealthreminder.alarm.AlarmService::class.java))
    }
}
