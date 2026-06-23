package com.example.smarthealthreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.features.model.Alarm
import java.util.Calendar

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm): Boolean {
        if (!canScheduleExactAlarm()) {
            Log.e("ALARM_HELPER", "Cannot schedule exact alarms")
            return false
        }

        val repeatDays = parseRepeatDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            scheduleOnce(alarm, alarm.id?.hashCode() ?: 0)
        } else {
            for (dayOfWeek in repeatDays) {
                scheduleRepeatingForDay(alarm, dayOfWeek)
            }
        }

        return true
    }

    private fun parseRepeatDays(repeatDays: String?): List<Int> {
        if (repeatDays.isNullOrBlank()) return emptyList()

        val dayNameMap = mapOf(
            "Sun" to Calendar.SUNDAY,
            "Mon" to Calendar.MONDAY,
            "Tue" to Calendar.TUESDAY,
            "Wed" to Calendar.WEDNESDAY,
            "Thu" to Calendar.THURSDAY,
            "Fri" to Calendar.FRIDAY,
            "Sat" to Calendar.SATURDAY
        )

        return repeatDays.split(" ", ",")
            .mapNotNull { dayNameMap[it.trim()] }
    }

    private fun scheduleOnce(alarm: Alarm, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            // ✅ استخدم ReminderReceiver بدل AlarmReceiver
            putExtra(ReminderReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, alarm.label)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, formatDisplayTime(alarm.time, alarm.amPm))
            putExtra(ReminderReceiver.EXTRA_TYPE, "alarm")
            putExtra(ReminderReceiver.EXTRA_VIBRATION, alarm.vibrationEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d("ALARM_HELPER", "Scheduling alarm ${alarm.id} at ${calendar.time}")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun scheduleRepeatingForDay(alarm: Alarm, dayOfWeek: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, alarm.label)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, formatDisplayTime(alarm.time, alarm.amPm))
            putExtra(ReminderReceiver.EXTRA_TYPE, "alarm")
            putExtra(ReminderReceiver.EXTRA_VIBRATION, alarm.vibrationEnabled)
            putExtra("repeat_day", dayOfWeek)
        }

        val requestCode = (alarm.id?.hashCode() ?: 0) + dayOfWeek

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    // Cancel by Alarm object
    fun cancelAlarm(alarm: Alarm) {
        val repeatDays = parseRepeatDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id?.hashCode() ?: 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } else {
            for (dayOfWeek in repeatDays) {
                val intent = Intent(context, ReminderReceiver::class.java)
                val requestCode = (alarm.id?.hashCode() ?: 0) + dayOfWeek
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    // Cancel by ID string
    fun cancelAlarm(alarmId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("ALARM_HELPER", "Cancelled alarm: $alarmId")
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int = 15): Boolean {
        if (!canScheduleExactAlarm()) return false

        // Cancel old alarm first
        cancelAlarm(alarm)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, alarm.label)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, "Snoozed - ${formatDisplayTime(alarm.time, alarm.amPm)}")
            putExtra(ReminderReceiver.EXTRA_TYPE, "alarm")
            putExtra(ReminderReceiver.EXTRA_VIBRATION, alarm.vibrationEnabled)
            putExtra("is_snooze", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id ?: "0").hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        Log.d("ALARM_HELPER", "Snoozing alarm ${alarm.id} for $minutes minutes")

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