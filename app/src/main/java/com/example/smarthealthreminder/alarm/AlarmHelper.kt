package com.example.smarthealthreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarthealthreminder.features.model.Alarm
import java.util.Calendar
import java.util.Locale

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an alarm after cancelling every existing PendingIntent for [alarm.id].
     * Thread-safe to avoid races with concurrent delete/toggle operations.
     */
    fun scheduleAlarm(alarm: Alarm): Boolean {
        if (!canScheduleExactAlarm()) return false
        val alarmId = alarm.id ?: return false

        synchronized(schedulingLock) {
            return try {
                cancelAllPendingIntents(alarmId)

                if (!alarm.isActive) return true

                val repeatDays = parseRepeatDays(alarm.repeatDays)
                if (repeatDays.isEmpty()) {
                    scheduleOnce(alarm)
                } else {
                    repeatDays.forEach { dayOfWeek ->
                        scheduleForDayOfWeek(alarm, dayOfWeek)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        alarm.id?.let { cancelAllPendingIntents(it) }
    }

    fun cancelAlarm(id: String) {
        cancelAllPendingIntents(id)
    }

    /**
     * Cancels the base, all seven day-specific, and snooze PendingIntents for an alarm.
     * Safe to call even when the alarm row no longer exists (orphan cleanup).
     */
    fun cancelAllPendingIntents(alarmId: String) {
        synchronized(schedulingLock) {
            cancelPendingIntent(baseRequestCode(alarmId))
            ALL_DAYS_OF_WEEK.forEach { day ->
                cancelPendingIntent(dayRequestCode(alarmId, day))
            }
            cancelPendingIntent(snoozeRequestCode(alarmId))
        }
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int = 15): Boolean {
        if (!canScheduleExactAlarm()) return false
        val alarmId = alarm.id ?: return false

        synchronized(schedulingLock) {
            cancelPendingIntent(snoozeRequestCode(alarmId))

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_ALARM_LABEL, alarm.label)
                putExtra(EXTRA_ALARM_TIME, formatDisplayTime(alarm.time, alarm.amPm))
                putExtra(EXTRA_ALARM_CATEGORY, alarm.category)
                putExtra(EXTRA_IS_SNOOZE, true)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                snoozeRequestCode(alarmId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
            return true
        }
    }

    fun cancelSnoozePendingIntent(alarmId: String?) {
        if (alarmId.isNullOrBlank()) return
        synchronized(schedulingLock) {
            cancelPendingIntent(snoozeRequestCode(alarmId))
        }
    }

    fun canScheduleExactAlarm(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleOnce(alarm: Alarm) {
        val alarmId = alarm.id ?: return
        val intent = buildAlarmIntent(alarm)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = nextTriggerMillis(alarm, Calendar.getInstance())
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun scheduleForDayOfWeek(alarm: Alarm, dayOfWeek: Int) {
        val alarmId = alarm.id ?: return
        val intent = buildAlarmIntent(alarm).apply {
            putExtra(EXTRA_REPEAT_DAY, dayOfWeek)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dayRequestCode(alarmId, dayOfWeek),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    /** Reschedules a single repeat-day occurrence one week after it fired. */
    fun rescheduleWeeklyOccurrence(alarm: Alarm, repeatDay: Int) {
        val alarmId = alarm.id ?: return
        if (!canScheduleExactAlarm()) return

        synchronized(schedulingLock) {
            val intent = buildAlarmIntent(alarm).apply {
                putExtra(EXTRA_REPEAT_DAY, repeatDay)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                dayRequestCode(alarmId, repeatDay),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, repeatDay)
                val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.WEEK_OF_YEAR, 1)
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun buildAlarmIntent(alarm: Alarm): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_TIME, formatDisplayTime(alarm.time, alarm.amPm))
            putExtra(EXTRA_ALARM_CATEGORY, alarm.category)
        }
    }

    private fun nextTriggerMillis(alarm: Alarm, from: Calendar): Long {
        val (hour, minute) = parseAlarmTime(alarm.time, alarm.amPm)
        val calendar = from.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun cancelPendingIntent(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    companion object {
        private val schedulingLock = Any()

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_TIME = "alarm_time"
        const val EXTRA_ALARM_CATEGORY = "alarm_category"
        const val EXTRA_REPEAT_DAY = "repeat_day"
        const val EXTRA_IS_SNOOZE = "is_snooze"

        private const val SNOOZE_REQUEST_OFFSET = 1000

        private val ALL_DAYS_OF_WEEK = intArrayOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )

        fun baseRequestCode(alarmId: String): Int = alarmId.hashCode()

        fun dayRequestCode(alarmId: String, dayOfWeek: Int): Int = alarmId.hashCode() + dayOfWeek

        fun snoozeRequestCode(alarmId: String): Int = alarmId.hashCode() + SNOOZE_REQUEST_OFFSET

        /**
         * Parses repeat-day strings in multiple formats (e.g. "Mon Tue", "mon,wed", "M W F").
         * Returns distinct Calendar day-of-week values; never throws on malformed input.
         */
        fun parseRepeatDays(repeatDays: String?): List<Int> {
            if (repeatDays.isNullOrBlank()) return emptyList()

            val dayNameMap = mapOf(
                "SUN" to Calendar.SUNDAY,
                "SUNDAY" to Calendar.SUNDAY,
                "MON" to Calendar.MONDAY,
                "MONDAY" to Calendar.MONDAY,
                "M" to Calendar.MONDAY,
                "TUE" to Calendar.TUESDAY,
                "TUESDAY" to Calendar.TUESDAY,
                "T" to Calendar.TUESDAY,
                "WED" to Calendar.WEDNESDAY,
                "WEDNESDAY" to Calendar.WEDNESDAY,
                "W" to Calendar.WEDNESDAY,
                "THU" to Calendar.THURSDAY,
                "THURSDAY" to Calendar.THURSDAY,
                "TH" to Calendar.THURSDAY,
                "FRI" to Calendar.FRIDAY,
                "FRIDAY" to Calendar.FRIDAY,
                "F" to Calendar.FRIDAY,
                "SAT" to Calendar.SATURDAY,
                "SATURDAY" to Calendar.SATURDAY,
                "SA" to Calendar.SATURDAY,
                "S" to Calendar.SUNDAY
            )

            return repeatDays
                .split(" ", ",", ";", "|")
                .map { it.trim().uppercase(Locale.US) }
                .filter { it.isNotEmpty() }
                .mapNotNull { token ->
                    dayNameMap[token] ?: runCatching { token.toInt() }.getOrNull()
                        ?.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }
                }
                .distinct()
                .sorted()
        }

        fun formatRepeatDays(days: List<Int>): String {
            val labels = mapOf(
                Calendar.SUNDAY to "Sun",
                Calendar.MONDAY to "Mon",
                Calendar.TUESDAY to "Tue",
                Calendar.WEDNESDAY to "Wed",
                Calendar.THURSDAY to "Thu",
                Calendar.FRIDAY to "Fri",
                Calendar.SATURDAY to "Sat"
            )
            return days.mapNotNull { labels[it] }.joinToString(" ")
        }

        fun parseAlarmTime(time: String?, amPm: String?): Pair<Int, Int> {
            val parts = time?.split(":").orEmpty()
            var hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

            if (hour !in 0..23 || minute !in 0..59) {
                return 8 to 0
            }

            val normalizedAmPm = amPm?.trim()?.uppercase(Locale.US)
            if (hour in 1..12 && (normalizedAmPm == "AM" || normalizedAmPm == "PM")) {
                hour = when {
                    hour == 12 && normalizedAmPm == "AM" -> 0
                    hour < 12 && normalizedAmPm == "PM" -> hour + 12
                    else -> hour
                }
            }

            return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
        }

        fun isValidTime(hour24: Int, minute: Int): Boolean {
            return hour24 in 0..23 && minute in 0..59
        }

        fun formatDisplayTime(time: String?, amPm: String?): String {
            val parts = time?.split(":").orEmpty()
            val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return time ?: ""
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return time ?: ""

            if (hour in 0..23 && amPm.isNullOrBlank()) {
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                val suffix = if (hour < 12) "AM" else "PM"
                return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
            }

            return if (amPm.isNullOrBlank()) {
                String.format(Locale.US, "%02d:%02d", hour, minute)
            } else {
                String.format(Locale.US, "%02d:%02d %s", hour, minute, amPm)
            }
        }
    }

    private fun parseAlarmTime(time: String?, amPm: String?): Pair<Int, Int> =
        Companion.parseAlarmTime(time, amPm)

    private fun formatDisplayTime(time: String?, amPm: String?): String =
        Companion.formatDisplayTime(time, amPm)
}
