package com.example.smarthealthreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.features.data.local.AppDatabase
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
                    rescheduleAlarms(context)
                    rescheduleReminders(context)
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

    private suspend fun rescheduleAlarms(context: Context) {
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
                Log.d("AlarmReceiver", "Rescheduled alarm: ${alarm.id}")
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to reschedule alarms", e)
        }
    }

    private suspend fun rescheduleReminders(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val reminders = db.reminderDao().getAllReminders().first()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val now = System.currentTimeMillis()

            reminders.filter { it.status == "Pending" || it.status == "Snoozed" }.forEach { reminder ->
                try {
                    val dateParts = reminder.date?.split("-") ?: return@forEach
                    val timeParts = reminder.time?.split(":") ?: return@forEach
                    if (dateParts.size < 3 || timeParts.size < 2) return@forEach

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, dateParts[0].toInt())
                        set(Calendar.MONTH, dateParts[1].toInt() - 1)
                        set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // If the reminder time is in the past, mark it as Missed
                    if (calendar.timeInMillis < now) {
                        db.reminderDao().updateReminderStatus(reminder.id, "Missed")
                        Log.d("AlarmReceiver", "Marked past reminder as missed: ${reminder.id}")
                        return@forEach
                    }

                    // Schedule the reminder
                    val intent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                        putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                        putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description ?: "Time for your health reminder!")
                        putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                        putExtra(ReminderReceiver.EXTRA_VIBRATION, reminder.vibrationEnabled)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminder.id.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Log.e("AlarmReceiver", "Cannot schedule exact alarms for reminder: ${reminder.id}")
                        return@forEach
                    }

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AlarmReceiver", "Rescheduled reminder: ${reminder.id} at ${reminder.date} ${reminder.time}")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to reschedule reminder: ${reminder.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to reschedule reminders", e)
        }
    }
}
