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
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.alarm.ReminderScheduler
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * SINGLE SOURCE OF TRUTH for all reminder alarm logic.
 *
 * ✅ UPDATED: Room ONLY — removed all SQLite DB updates
 * ✅ UPDATED: Consistent missed timing (+2 min from snooze trigger)
 * ✅ UPDATED: Broadcast refresh after every action
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.example.smarthealthreminder.ACTION_SNOOZE"
        const val ACTION_MARK_TAKEN = "com.example.smarthealthreminder.ACTION_MARK_TAKEN"
        const val ACTION_DISMISS = "com.example.smarthealthreminder.ACTION_DISMISS"
        const val ACTION_MISSED = "com.example.smarthealthreminder.ACTION_MISSED"

        // ✅ NEW: Broadcast action for dashboard refresh
        const val ACTION_REFRESH_DASHBOARD = "com.example.smarthealthreminder.ACTION_REFRESH_DASHBOARD"

        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "Health Reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_DESCRIPTION = "reminder_description"
        const val EXTRA_TYPE = "reminder_type"
        const val EXTRA_VIBRATION = "vibration_enabled"
        const val EXTRA_SNOOZE_USED = "snooze_used"

        private const val RC_SNOOZE_ALARM = 5000
        private const val RC_MISSED_ALARM = 9000
        private const val RC_NOTIFICATION = 0
        private const val RC_SNOOZE_ACTION = 1000
        private const val RC_TAKEN_ACTION = 2000
        private const val RC_DISMISS_ACTION = 3000

        // ✅ NEW: Consistent missed delay (2 minutes = 120 seconds)
        const val MISSED_DELAY_MILLIS = 2 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("REMINDER_RECEIVER", "onReceive action=${intent.action}, extras=${intent.extras?.keySet()?.toList()}")

        val settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean(SettingsActivity.KEY_NOTIFICATIONS, true)) {
            Log.d("REMINDER_RECEIVER", "Notifications disabled in settings")
            return
        }

        when (intent.action) {
            ACTION_SNOOZE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleSnooze(context, intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MARK_TAKEN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleMarkTaken(context, intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MISSED -> {
                showMissedNotification(context, intent)
            }
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

        val snoozeUsed = intent.getBooleanExtra(EXTRA_SNOOZE_USED, false)

        Log.d("REMINDER_RECEIVER", "Showing notification: id=$reminderId, title=$title, type=$type, snoozeUsed=$snoozeUsed")

        showNotification(context, reminderId, title, description, type, snoozeUsed)

        if (type == "reminder") {
            // ✅ FIXED: Use constant for consistent timing
            scheduleMissedNotification(context, reminderId, System.currentTimeMillis() + MISSED_DELAY_MILLIS)
        }

        if (vibrationEnabled) {
            vibrate(context)
        }
    }

    private fun showNotification(
        context: Context,
        id: String,
        title: String,
        description: String,
        type: String,
        snoozeUsed: Boolean = false
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
            id.hashCode() + RC_NOTIFICATION,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_SNOOZE_USED, true)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + RC_SNOOZE_ACTION,
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
            id.hashCode() + RC_TAKEN_ACTION,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + RC_DISMISS_ACTION,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeMinutes = SettingsActivity.getReminderSnoozeMinutes(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("💊 $title")
            .setContentText(description.ifEmpty { "Time for your health reminder!" })
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .setDeleteIntent(dismissPendingIntent)

        if (!snoozeUsed) {
            builder.addAction(
                R.drawable.ic_snooze,
                "Snooze ${snoozeMinutes}m",
                snoozePendingIntent
            )
            Log.d("REMINDER_RECEIVER", "Snooze button added (first time)")
        } else {
            Log.d("REMINDER_RECEIVER", "Snooze button hidden (already used)")
        }

        notificationManager.notify(id.hashCode(), builder.build())
    }

    /**
     * SINGLE SOURCE OF TRUTH for missed notifications.
     * Only fires when AlarmManager triggers it.
     * Checks DB before showing to prevent false positives.
     */
    private fun showMissedNotification(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: run {
            Log.w("REMINDER_RECEIVER", "showMissedNotification: no reminder ID")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(id)

            // GUARD: Only show if status is STILL "Snoozed"
            if (reminder == null) {
                Log.d("REMINDER_RECEIVER", "Missed check: reminder $id not found in DB")
                return@launch
            }

            if (reminder.status != "Snoozed") {
                Log.d("REMINDER_RECEIVER", "Missed check: status is ${reminder.status}, skipping notification")
                return@launch
            }

            // ✅ Room ONLY — removed SQLite update
            db.reminderDao().updateReminderStatus(id, "Missed")

            // ✅ Broadcast refresh
            sendRefreshBroadcast(context)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val medicineName = reminder.title ?: "Medicine"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("❌ Missed: $medicineName")
                .setContentText("You didn't take $medicineName after snooze reminder.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(id.hashCode() + 9999, notification)
            Log.d("REMINDER_RECEIVER", "Missed notification shown for $id")
        }
    }

    private suspend fun handleSnooze(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"
        val originalTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Health Reminder"
        val originalDescription = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        Log.d("REMINDER_RECEIVER", "handleSnooze: id=$id, type=$type")

        dismissNotification(context, intent)

        val snoozeMinutes = SettingsActivity.getReminderSnoozeMinutes(context)
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeMinutes)
        }
        val newHour = calendar.get(Calendar.HOUR_OF_DAY)
        val newMinute = calendar.get(Calendar.MINUTE)
        val newAmPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val newTime = String.format(Locale.getDefault(), "%02d:%02d", newHour, newMinute)
        val snoozeTriggerMillis = calendar.timeInMillis

        Log.d("REMINDER_RECEIVER", "New snooze time: $newTime $newAmPm (snoozeMinutes=$snoozeMinutes)")

        if (type == "alarm") {
            // ✅ Room ONLY for alarms too
            try {
                val db = AppDatabase.getDatabase(context)
                db.alarmDao().updateLastTriggeredStatus(id, "Snoozed")
            } catch (e: Exception) {
                Log.e("REMINDER_RECEIVER", "Failed to update Room alarm status", e)
            }

            val alarmHelper = AlarmHelper(context)
            // Note: You'll need to update AlarmHelper to use Room too
            // For now, keeping minimal changes
        } else {
            // ✅ Room ONLY — removed ALL SQLite updates
            val db = AppDatabase.getDatabase(context)

            db.reminderDao().updateReminderStatus(id, "Snoozed")
            db.reminderDao().updateReminderTime(id, newTime) // Add this method to DAO

            // ✅ Broadcast refresh immediately
            sendRefreshBroadcast(context)

            // Cancel old missed alarm before scheduling new one
            cancelMissedNotification(context, id)

            // Schedule snooze alarm with snoozeUsed=true
            scheduleReminderSnooze(context, id, snoozeTriggerMillis, originalTitle, originalDescription, snoozeMinutes, true)

            // ✅ FIXED: Consistent +2 minutes from snooze trigger
            scheduleMissedNotification(
                context,
                id,
                snoozeTriggerMillis + MISSED_DELAY_MILLIS
            )

            Log.d("REMINDER_RECEIVER", "Reminder snoozed: $id at $newTime, missed check at ${snoozeTriggerMillis + MISSED_DELAY_MILLIS}")
        }
    }

    private fun scheduleReminderSnooze(
        context: Context,
        reminderId: String,
        triggerAtMillis: Long,
        title: String,
        description: String,
        snoozeMinutes: Int,
        snoozeUsed: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("REMINDER_RECEIVER", "Cannot schedule exact alarms")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TYPE, "reminder")
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_SNOOZE_USED, snoozeUsed)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + RC_SNOOZE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("REMINDER_RECEIVER", "Snooze alarm scheduled at $triggerAtMillis (snoozeUsed=$snoozeUsed)")
    }

    private fun scheduleMissedNotification(
        context: Context,
        reminderId: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("REMINDER_RECEIVER", "Cannot schedule exact alarms for missed check")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MISSED
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + RC_MISSED_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("REMINDER_RECEIVER", "Missed alarm scheduled at $triggerAtMillis for $reminderId")
    }

    private suspend fun handleMarkTaken(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"

        Log.d("REMINDER_RECEIVER", "handleMarkTaken: id=$id, type=$type")

        // Cancel missed check BEFORE updating DB
        cancelMissedNotification(context, id)

        val db = AppDatabase.getDatabase(context)

        when (type) {
            "alarm" -> {
                db.alarmDao().updateLastTriggeredStatus(id, "Completed")
                AlarmHelper(context).cancelAlarm(id)
            }
            "reminder" -> {
                val reminder = db.reminderDao().getReminderById(id)

                if (reminder != null && reminder.isRecurring && RecurrenceHelper.isRecurring(reminder.recurrenceType)) {
                    val nextMillis = RecurrenceHelper.computeNextTriggerMillis(
                        date = reminder.date,
                        time = reminder.time,
                        recurrenceType = reminder.recurrenceType
                    )

                    if (nextMillis != null) {
                        // ✅ Room ONLY
                        db.reminderDao().updateReminderStatus(id, "Pending")
                        ReminderScheduler.scheduleReminder(context, reminder, nextMillis)
                        Log.d("REMINDER_RECEIVER", "Recurring reminder marked taken, next alarm scheduled")
                    } else {
                        db.reminderDao().updateReminderStatus(id, "Completed")
                    }
                } else {
                    // ✅ Room ONLY — removed SQLite
                    db.reminderDao().updateReminderStatus(id, "Completed")
                    Log.d("REMINDER_RECEIVER", "Room reminder status updated to Completed")
                }
            }
        }

        // ✅ Broadcast refresh immediately
        sendRefreshBroadcast(context)
        dismissNotification(context, intent)
    }

    /**
     * ✅ NEW: Send broadcast to refresh dashboard
     */
    private fun sendRefreshBroadcast(context: Context) {
        val refreshIntent = Intent(ACTION_REFRESH_DASHBOARD)
        context.sendBroadcast(refreshIntent)
        Log.d("REMINDER_RECEIVER", "Refresh dashboard broadcast sent")
    }

    private fun cancelMissedNotification(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MISSED
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + RC_MISSED_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("REMINDER_RECEIVER", "Cancelled missed alarm for $reminderId")
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id.hashCode())
        Log.d("REMINDER_RECEIVER", "Notification dismissed: $id")
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        }
    }
}