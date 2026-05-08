package com.example.smarthealthreminder.features.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.alarm.AlarmService
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
    private var alarmLabel: String = "Alarm"
    private var alarmTime: String = ""
    private var alarmCategory: String = ""
    private lateinit var alarmHelper: AlarmHelper
    private var isStopped = false  // ← عشان نمنع onDestroy يوقف الـ snooze

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_ringing)

        alarmHelper = AlarmHelper(this)

        alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        alarmTime = intent.getStringExtra(EXTRA_ALARM_TIME) ?: getCurrentTime()
        alarmCategory = intent.getStringExtra(EXTRA_ALARM_CATEGORY) ?: "General"
        val priority = intent.getStringExtra(EXTRA_ALARM_PRIORITY) ?: "NORMAL"
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: ""
        val roomInfo = intent.getStringExtra(EXTRA_ROOM_INFO) ?: ""

        findViewById<TextView>(R.id.tv_alarm_time)?.text = alarmTime
        findViewById<TextView>(R.id.tv_alarm_date)?.text = getCurrentDate()
        findViewById<TextView>(R.id.tv_alarm_label)?.text = alarmLabel
        findViewById<TextView>(R.id.tv_alarm_category)?.text = alarmCategory
        findViewById<TextView>(R.id.tv_alarm_priority)?.text = priority
        findViewById<TextView>(R.id.tv_patient_name)?.text =
            if (patientName.isNotEmpty()) "Patient: $patientName" else ""
        findViewById<TextView>(R.id.tv_room_info)?.text = roomInfo

        findViewById<Button>(R.id.btn_snooze)?.setOnClickListener {
            isStopped = true
            stopAlarmService()  // ← وقفي الصوت
            // شيديل snooze بعد 10 دقايق
            alarmId?.let { id ->
                val snoozeAlarm = com.example.smarthealthreminder.features.model.Alarm(
                    id = id,
                    label = alarmLabel,
                    time = alarmTime,
                    amPm = "",
                    category = alarmCategory
                )
                alarmHelper.snoozeAlarm(snoozeAlarm, 10)
            }
            finish()
        }

        findViewById<Button>(R.id.btn_stop_alarm)?.setOnClickListener {
            isStopped = true
            stopAlarmService()  // ← وقفي الصوت
            finish()
        }

        // ← مفيش startService هنا خالص
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isStopped) {
            stopAlarmService()
        }
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
        val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time).uppercase()
    }
}