package com.example.smarthealthreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.features.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarms = db.alarmDao().getAllAlarms().first()
                    val alarmHelper = AlarmHelper(context)
                    alarms.filter { it.isActive }.forEach { entity ->
                        val alarm = Alarm(
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
                        alarmHelper.scheduleAlarm(alarm)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val alarmId = intent.getStringExtra("alarm_id")
        if (alarmId.isNullOrBlank()) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isSnooze = intent.getBooleanExtra("is_snooze", false)
                val repeatDay = intent.getIntExtra("repeat_day", -1)

                val alarm = AppDatabase.getDatabase(context)
                    .alarmDao()
                    .getActiveAlarmById(alarmId)

                if (alarm == null && !isSnooze) {
                    return@launch
                }

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("alarm_label", alarm?.label ?: intent.getStringExtra("alarm_label"))
                    putExtra("alarm_time", intent.getStringExtra("alarm_time") ?: alarm?.time)
                    putExtra("alarm_category", alarm?.category ?: intent.getStringExtra("alarm_category"))
                    putExtra("is_snooze", isSnooze)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                if (repeatDay != -1 && !isSnooze && alarm != null) {
                    val rescheduleIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("alarm_id", alarmId)
                        putExtra("alarm_label", alarm.label)
                        putExtra("alarm_time", intent.getStringExtra("alarm_time") ?: alarm.time)
                        putExtra("alarm_category", alarm.category)
                        putExtra("repeat_day", repeatDay)
                    }

                    val requestCode = alarmId.hashCode() + repeatDay
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        rescheduleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val timeStr = alarm.time
                    val parts = timeStr.split(":")
                    var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

                    val amPm = alarm.amPm
                    if (hour in 1..12 && (amPm == "AM" || amPm == "PM")) {
                        hour = when {
                            hour == 12 && amPm == "AM" -> 0
                            hour < 12 && amPm == "PM" -> hour + 12
                            else -> hour
                        }
                    }

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, repeatDay)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

            } finally {
                pendingResult.finish()
            }
        }
    }
}
