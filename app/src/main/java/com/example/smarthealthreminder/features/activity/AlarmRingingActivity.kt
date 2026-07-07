package com.example.smarthealthreminder.features.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.alarm.AlarmService
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmRingingActivity : BaseActivity() {

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
    private lateinit var viewModel: HealthViewModel
    private var isStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_ringing)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        alarmHelper = AlarmHelper(this)

        val repository = HealthRepository(AppDatabase.getDatabase(this))
        viewModel = ViewModelProvider(this, HealthViewModelFactory(repository))[HealthViewModel::class.java]

        alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId.isNullOrBlank()) {
            stopAlarmService()
            finish()
            return
        }

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

        // ✅ Read the snooze duration ONCE, right when the screen opens
        val snoozeMinutes = SettingsPrefs.getAlarmSnoozeMinutes(this)

        // ✅ Set the button label immediately on create — not after clicking
        findViewById<Button>(R.id.btn_snooze)?.text =
            getString(R.string.snooze_button_label, snoozeMinutes)

        findViewById<Button>(R.id.btn_snooze)?.setOnClickListener {
            // ✅ Reuse the same snoozeMinutes value read in onCreate — no re-read here
            val snoozed = alarmId?.let { id ->
                val parts = alarmTime.trim().split(" ")
                val timePart = parts.getOrNull(0) ?: alarmTime
                val amPmPart = parts.getOrNull(1) ?: ""

                val snoozeAlarm = com.example.smarthealthreminder.features.model.Alarm(
                    id = id,
                    label = alarmLabel,
                    time = timePart,
                    amPm = amPmPart,
                    category = alarmCategory
                )
                alarmHelper.snoozeAlarm(snoozeAlarm, snoozeMinutes)
            } ?: false

            if (snoozed) {
                isStopped = true
                alarmId?.let { viewModel.markAlarmSnoozed(it, snoozeMinutes) }
                stopAlarmService()
                Toast.makeText(
                    this,
                    getString(R.string.snoozed_for_minutes, snoozeMinutes),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Couldn't snooze. Please allow exact alarms or stop the alarm.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        findViewById<Button>(R.id.btn_stop_alarm)?.setOnClickListener {
            isStopped = true
            alarmId?.let { viewModel.markAlarmCompleted(it) }
            stopAlarmService()
            finish()
        }
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
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time).uppercase()
    }
}