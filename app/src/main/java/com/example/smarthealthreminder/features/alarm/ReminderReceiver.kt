package com.example.smarthealthreminder.features.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.features.data_d.DatabaseHelper
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import java.util.Calendar
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.example.smarthealthreminder.ACTION_SNOOZE"
        const val ACTION_MARK_TAKEN = "com.example.smarthealthreminder.ACTION_MARK_TAKEN"
        const val ACTION_DISMISS = "com.example.smarthealthreminder.ACTION_DISMISS"
        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "Health Reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_DESCRIPTION = "reminder_description"
        const val EXTRA_TYPE = "reminder_type"
        const val EXTRA_VIBRATION = "vibration_enabled"
        const val SNOOZE_MINUTES = 15
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("REMINDER_RECEIVER", "onReceive action=${intent.action}, extras=${intent.extras?.keySet()?.toList()}")

        val settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean(SettingsActivity.KEY_NOTIFICATIONS, true)) {
            Log.d("REMINDER_RECEIVER", "Notifications disabled in settings")
            return
        }

        when (intent.action) {
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_MARK_TAKEN -> handleMarkTaken(context, intent)
            ACTION_DISMISS -> dismissNotification(context, intent)
            else -> handleAlarmTrigger(context, intent)
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
            ?: intent.getStringExtra(EXTRA_ALARM_ID)
            ?: intent.getStringExtra("alarm_id")
            ?: return

        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: intent.getStringExtra("alarm_label")
            ?: intent.getStringExtra("reminder_title")
            ?: "Health Reminder"

        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
            ?: intent.getStringExtra("alarm_time")
            ?: intent.getStringExtra("reminder_description")
            ?: ""

        val type = intent.getStringExtra(EXTRA_TYPE)
            ?: if (intent.hasExtra("alarm_id")) "alarm" else "reminder"

        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION, false) &&
                context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(SettingsActivity.KEY_VIBRATION, true)

        Log.d("REMINDER_RECEIVER", "Showing notification: id=$reminderId, title=$title, type=$type")

        showNotification(context, reminderId, title, description, type)

        if (vibrationEnabled) {
            vibrate(context)
        }
    }

    private fun showNotification(
        context: Context,
        id: String,
        title: String,
        description: String,
        type: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for health reminders"
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 500, 200, 500)
            channel.setBypassDnd(true)
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_HOME)
            putExtra("OPEN_FROM_NOTIFICATION", true)
            putExtra(EXTRA_REMINDER_ID, id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TYPE, type)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TYPE, type)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 2000,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 3000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("💊 $title")
            .setContentText(description.ifEmpty { "Time for your health reminder!" })
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze ${SNOOZE_MINUTES}m", snoozePendingIntent)
            .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"

        Log.d("REMINDER_RECEIVER", "handleSnooze: id=$id, type=$type")

        dismissNotification(context, intent)

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, SNOOZE_MINUTES)
        }
        val newHour = calendar.get(Calendar.HOUR_OF_DAY)
        val newMinute = calendar.get(Calendar.MINUTE)
        val newAmPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val newTime = String.format(Locale.getDefault(), "%02d:%02d", newHour, newMinute)

        Log.d("REMINDER_RECEIVER", "New snooze time: $newTime $newAmPm")

        val dbHelper = DatabaseHelper(context)

        if (type == "alarm") {
            val success = dbHelper.snoozeAlarmByTime(id, newTime, newAmPm)
            if (!success) {
                Log.e("REMINDER_RECEIVER", "Failed to update database for snooze")
                return
            }

            val alarmHelper = AlarmHelper(context)
            val updatedAlarm = dbHelper.getAlarmById(id)

            if (updatedAlarm != null) {
                alarmHelper.cancelAlarm(id)
                alarmHelper.scheduleAlarm(updatedAlarm)
                Log.d("REMINDER_RECEIVER", "Alarm snooze scheduled: $id at $newTime $newAmPm")
            }
        } else {
            // ✅ Use updateReminderStatus instead of updateReminderStatusSnoozed
            val success = dbHelper.updateReminderStatus(id, "Snoozed")
            if (!success) {
                Log.e("REMINDER_RECEIVER", "Failed to update reminder status to Snoozed")
                return
            }

            // Update time in database for the snoozed reminder
            val values = android.content.ContentValues().apply {
                put("time", newTime)
            }
            val db = dbHelper.writableDatabase
            db.update("reminders", values, "id = ?", arrayOf(id))
            db.close()

            scheduleReminderSnooze(context, id, calendar.timeInMillis)
            Log.d("REMINDER_RECEIVER", "Reminder snoozed: $id at $newTime")
        }
    }

    private fun scheduleReminderSnooze(context: Context, reminderId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("REMINDER_RECEIVER", "Cannot schedule exact alarms")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TYPE, "reminder")
            putExtra(EXTRA_TITLE, "Snoozed Reminder")
            putExtra(EXTRA_DESCRIPTION, "Your snoozed reminder is ringing!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("REMINDER_RECEIVER", "Reminder snooze scheduled at $triggerAtMillis")
    }

    private fun handleMarkTaken(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"

        Log.d("REMINDER_RECEIVER", "handleMarkTaken: id=$id, type=$type")

        val dbHelper = DatabaseHelper(context)

        when (type) {
            "alarm" -> {
                dbHelper.markAlarmAsTaken(id)
                AlarmHelper(context).cancelAlarm(id)
            }
            "reminder" -> {
                dbHelper.updateReminderStatus(id, "Done")
            }
        }

        dismissNotification(context, intent)
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id.hashCode())
        Log.d("REMINDER_RECEIVER", "Notification dismissed: $id")
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
        }
    }
}