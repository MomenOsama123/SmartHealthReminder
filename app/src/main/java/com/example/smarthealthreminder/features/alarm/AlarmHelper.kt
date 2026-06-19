package com.example.smarthealthreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarthealthreminder.features.model.Alarm
import java.util.Calendar

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm): Boolean {
        if (!canScheduleExactAlarm()) return false

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_time", formatDisplayTime(alarm.time, alarm.amPm))
            putExtra("alarm_category", alarm.category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time already passed today, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        return true
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent?.cancel()
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int = 10): Boolean {
        if (!canScheduleExactAlarm()) return false

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_time", formatDisplayTime(alarm.time, alarm.amPm))
            putExtra("alarm_category", alarm.category)
            putExtra("is_snooze", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id ?: "0").hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTime,
            pendingIntent
        )

        return true
    }

    fun canScheduleExactAlarm(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun parseAlarmTime(time: String?, amPm: String?): Pair<Int, Int> {
        val parts = time?.split(":").orEmpty()
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (hour in 1..12 && (amPm == "AM" || amPm == "PM")) {
            hour = when {
                hour == 12 && amPm == "AM" -> 0
                hour < 12 && amPm == "PM" -> hour + 12
                else -> hour
            }
        }

        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    private fun formatDisplayTime(time: String?, amPm: String?): String {
        val parts = time?.split(":").orEmpty()
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time ?: ""
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return time ?: ""

        if (hour in 0..23 && amPm.isNullOrBlank()) {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val suffix = if (hour < 12) "AM" else "PM"
            return String.format("%02d:%02d %s", displayHour, minute, suffix)
        }

        return if (amPm.isNullOrBlank()) {
            String.format("%02d:%02d", hour, minute)
        } else {
            String.format("%02d:%02d %s", hour, minute, amPm)
        }
    }
}
