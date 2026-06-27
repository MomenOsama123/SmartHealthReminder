package com.example.smarthealthreminder.features.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.util.RecurrenceHelper

object ReminderScheduler {

    private const val EARLY_NOTIFICATION_REQUEST_OFFSET = 10_000

    fun scheduleReminder(
        context: Context,
        reminder: ReminderEntity,
        triggerAtMillis: Long,
        title: String = reminder.title,
        description: String = reminder.description ?: "Time for your health reminder!"
    ) {
        scheduleAlarm(
            context = context,
            reminder = reminder,
            triggerAtMillis = triggerAtMillis,
            requestCode = reminder.id.hashCode(),
            title = title,
            description = description
        )

        if (reminder.earlyNotification && reminder.earlyNotificationMinutes > 0) {
            val earlyTimeMillis = triggerAtMillis - reminder.earlyNotificationMinutes * 60 * 1000L
            if (earlyTimeMillis > System.currentTimeMillis()) {
                scheduleAlarm(
                    context = context,
                    reminder = reminder,
                    triggerAtMillis = earlyTimeMillis,
                    requestCode = reminder.id.hashCode() + EARLY_NOTIFICATION_REQUEST_OFFSET,
                    title = reminder.title,
                    description = "Upcoming in ${reminder.earlyNotificationMinutes} minutes"
                )
            }
        }
    }

    fun scheduleNextOccurrence(context: Context, reminder: ReminderEntity): Boolean {
        val nextMillis = RecurrenceHelper.computeNextTriggerMillis(
            date = reminder.date,
            time = reminder.time,
            recurrenceType = reminder.recurrenceType
        ) ?: return false

        scheduleReminder(context, reminder, nextMillis)
        return true
    }

    fun cancelReminderAlarms(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCodes = listOf(
            reminderId.hashCode(),
            reminderId.hashCode() + EARLY_NOTIFICATION_REQUEST_OFFSET,
            reminderId.hashCode() + 5000
        )

        requestCodes.forEach { requestCode ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        reminder: ReminderEntity,
        triggerAtMillis: Long,
        requestCode: Int,
        title: String,
        description: String
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, description)
            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
            putExtra(ReminderReceiver.EXTRA_VIBRATION, reminder.vibrationEnabled)
            putExtra("reminder_time", reminder.time)
            putExtra("is_recurring", reminder.isRecurring)
            putExtra("recurrence_type", reminder.recurrenceType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
