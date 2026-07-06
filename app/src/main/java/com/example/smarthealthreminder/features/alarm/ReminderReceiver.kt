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
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.example.smarthealthreminder.features.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import com.example.smarthealthreminder.features.data.local.entity.DoseLogEntity
import java.util.UUID

/**
 * SINGLE SOURCE OF TRUTH for all reminder alarm logic.
 *
 * ✅ FIXED: Alarm scheduling OUTSIDE Coroutine (runs on main thread)
 * ✅ FIXED: Snooze notification resets status to Pending before scheduling warning/missed
 * ✅ REMOVED: Dismiss action from notification
 * ✅ UPDATED: Snooze notification has NO buttons
 * ✅ UPDATED: Two-phase missed handling (Warning → Missed)
 * ✅ FIXED (new): handleAlarmTrigger now runs inside goAsync()+coroutine so the
 *    "Pending" status write ALWAYS completes before the warning alarm is scheduled/fires.
 *    This removes the race condition that made the post-snooze Warning silently
 *    disappear (it used to check status=="Pending" before the DB write had landed).
 * ✅ FIXED (new): Warning notification text now shows the CORRECT remaining time
 *    before Missed (8 minutes for a normal reminder, 2 minutes after a Snooze),
 *    instead of always hardcoding "2 minutes".
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL = "com.example.smarthealthreminder.ACTION_CANCEL"

        private const val RC_CANCEL_ACTION = 3000
        const val ACTION_SNOOZE = "com.example.smarthealthreminder.ACTION_SNOOZE"
        const val ACTION_MARK_TAKEN = "com.example.smarthealthreminder.ACTION_MARK_TAKEN"
        const val ACTION_WARNING = "com.example.smarthealthreminder.ACTION_WARNING"
        const val ACTION_MISSED = "com.example.smarthealthreminder.ACTION_MISSED"

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
        const val EXTRA_MISSED_MINUTES = "missed_minutes" // NEW: how many minutes left until Missed, for the warning text

        private const val RC_SNOOZE_ALARM = 5000
        private const val RC_WARNING_ALARM = 7000
        private const val RC_MISSED_ALARM = 9000
        private const val RC_NOTIFICATION = 0
        private const val RC_SNOOZE_ACTION = 1000
        private const val RC_TAKEN_ACTION = 2000

        // Timing constants
        const val WARNING_DELAY_MILLIS = 2 * 60 * 1000L      // 2 minutes - normal flow: trigger -> warning
        const val MISSED_DELAY_MILLIS = 10 * 60 * 1000L      // 10 minutes from original time - normal flow: trigger -> missed
        // Derived: normal flow gap between Warning and Missed = (10 - 2) = 8 minutes
        const val NORMAL_WARNING_TO_MISSED_MINUTES = ((MISSED_DELAY_MILLIS - WARNING_DELAY_MILLIS) / 60000L).toInt()
        // Snooze flow: warning fires immediately when "snooze over" notification appears,
        // and Missed happens WARNING_DELAY_MILLIS (2 min) later.
        const val SNOOZE_WARNING_TO_MISSED_MINUTES = (WARNING_DELAY_MILLIS / 60000L).toInt()
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("REMINDER_RECEIVER", "onReceive action=${intent.action}, extras=${intent.extras?.keySet()?.toList()}")

        val settings = context.getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean(SettingsPrefs.KEY_NOTIFICATIONS, true)) {
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
            ACTION_WARNING -> {
                showWarningNotification(context, intent)
            }
            ACTION_CANCEL -> {
                dismissNotification(context, intent)
            }
            ACTION_MISSED -> {
                handleMissedConversion(context, intent)
            }
            else -> {
                // ✅ FIX: run inside goAsync()+coroutine so that when snoozeUsed=true,
                // the "Pending" DB write finishes BEFORE we schedule/fire the warning alarm.
                // This is what used to race and silently swallow the post-snooze warning.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleAlarmTrigger(context, intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun handleAlarmTrigger(context: Context, intent: Intent) {
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
                context.getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(SettingsPrefs.KEY_VIBRATION, true)

        val snoozeUsed = intent.getBooleanExtra(EXTRA_SNOOZE_USED, false)

        Log.d("REMINDER_RECEIVER", "Showing notification: id=$reminderId, title=$title, type=$type, snoozeUsed=$snoozeUsed")

        // When snoozeUsed=true this is already the "second chance" -> show Taken+Cancel only (no Snooze button)
        showNotification(context, reminderId, title, description, type, snoozeUsed, warningShown = snoozeUsed)

        if (type == "reminder") {
            if (snoozeUsed) {
                // ✅ FIXED: await the status reset to Pending BEFORE scheduling the
                // (immediate) warning alarm, so there is no race where the warning
                // fires and reads a stale (non-Pending) status and gets skipped.
                val db = AppDatabase.getDatabase(context)
                db.reminderDao().updateReminderStatus(reminderId, "Pending")
                sendRefreshBroadcast(context)
                Log.d("REMINDER_RECEIVER", "Snooze notification: status reset to Pending for $reminderId")

                // ✅ Show the Warning notification RIGHT NOW, together with the "snooze is over"
                // notification (no separate scheduled alarm, no buttons on it).
                showImmediateSnoozeWarningNotification(context, reminderId, title)

                // Missed conversion still scheduled for 2 minutes from now.
                val missedTime = System.currentTimeMillis() + WARNING_DELAY_MILLIS
                scheduleMissedConversion(context, reminderId, missedTime)

                Log.d("REMINDER_RECEIVER", "Snooze missed-conversion scheduled at $missedTime (2 min, warning shown immediately)")
            } else {
                // Normal reminder: warning after 2 min, missed after 10 min total
                // (i.e. 8 minutes after the warning itself).
                scheduleWarningNotification(
                    context,
                    reminderId,
                    title,
                    System.currentTimeMillis() + WARNING_DELAY_MILLIS
                )
                scheduleMissedConversion(context, reminderId, System.currentTimeMillis() + MISSED_DELAY_MILLIS)
            }
        }

        if (vibrationEnabled) {
            vibrate(context)
        }
    }

    /**
     * Show main reminder notification.
     * If warningShown = false → shows Taken + Snooze + Cancel buttons.
     * If warningShown = true  → shows Taken + Cancel only (Snooze removed).
     * If snoozeUsed = true    → title = "⏰ Snooze is over time to take {title}"
     */
    private fun showNotification(
        context: Context,
        id: String,
        title: String,
        description: String,
        type: String,
        snoozeUsed: Boolean = false,
        warningShown: Boolean = false
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

        val finalTitle = if (snoozeUsed) {
            "⏰ Snooze is over time to take $title"
        } else {
            "💊 $title"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(finalTitle)
            .setContentText(description.ifEmpty { "Time for your health reminder!" })
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)

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

        val cancelIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_REMINDER_ID, id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + RC_CANCEL_ACTION,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!warningShown) {
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

            val snoozeMinutes = SettingsPrefs.getReminderSnoozeMinutes(context)

            builder
                .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
                .addAction(R.drawable.ic_snooze, "Snooze ${snoozeMinutes}m", snoozePendingIntent)
                .addAction(R.drawable.ic_close, "Cancel", cancelPendingIntent)

            Log.d("REMINDER_RECEIVER", "Normal reminder: Taken + Snooze + Cancel buttons shown")
        } else {
            builder
                .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
                .addAction(R.drawable.ic_close, "Cancel", cancelPendingIntent)

            Log.d("REMINDER_RECEIVER", "Warning shown: Taken + Cancel buttons only")
        }

        notificationManager.notify(id.hashCode(), builder.build())
    }

    /**
     * Shows the Warning notification IMMEDIATELY (no AlarmManager scheduling, no DB status
     * check) right when the "snooze is over" notification appears. No buttons on it —
     * it's purely informational: "if not taken within 2 minutes, it will be Missed."
     */
    private fun showImmediateSnoozeWarningNotification(context: Context, id: String, medicineName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("⚠️ Warning: $medicineName")
            .setContentText("If you don't take $medicineName within $SNOOZE_WARNING_TO_MISSED_MINUTES minutes, it will be marked as Missed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.hashCode() + 7777, notification)
        Log.d("REMINDER_RECEIVER", "Immediate post-snooze warning notification shown for $id")
    }

    /**
     * PHASE 1: Warning notification.
     * Shows warning ONLY. Status stays Pending.
     * The text now dynamically shows how many minutes are left before Missed,
     * read from EXTRA_MISSED_MINUTES (8 for the normal flow, 2 after a Snooze).
     */
    private fun showWarningNotification(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: run {
            Log.w("REMINDER_RECEIVER", "showWarningNotification: no reminder ID")
            return
        }

        val medicineName = intent.getStringExtra(EXTRA_TITLE) ?: "Medicine"
        val missedMinutes = intent.getIntExtra(EXTRA_MISSED_MINUTES, NORMAL_WARNING_TO_MISSED_MINUTES)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(id)

            if (reminder == null) {
                Log.d("REMINDER_RECEIVER", "Warning check: reminder $id not found in DB")
                return@launch
            }

            Log.d("TEST", "Warning check: Current status = ${reminder.status}")

            if (reminder.status != "Pending") {
                Log.d("REMINDER_RECEIVER", "Warning check: status is ${reminder.status}, skipping warning")
                return@launch
            }

            // ✅ WARNING ONLY — do NOT change status here!
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("⚠️ Warning: $medicineName")
                .setContentText("If you don't take $medicineName within $missedMinutes minutes, it will be marked as Missed.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(id.hashCode() + 7777, notification)
            Log.d("REMINDER_RECEIVER", "Warning notification shown for $id — status remains Pending (missedMinutes=$missedMinutes)")

            // ✅ Update main notification: remove Snooze button, keep Taken + Cancel
            showNotification(
                context,
                id,
                medicineName,
                reminder.description ?: "",
                "reminder",
                snoozeUsed = false,
                warningShown = true
            )
            Log.d("REMINDER_RECEIVER", "Main notification updated for $id — Snooze removed")
        }
    }

    /**
     * PHASE 2: Convert to Missed.
     * Only converts if status is STILL "Pending".
     */
    private fun handleMissedConversion(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: run {
            Log.w("REMINDER_RECEIVER", "handleMissedConversion: no reminder ID")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(id)

            if (reminder == null) {
                Log.d("REMINDER_RECEIVER", "Missed conversion: reminder $id not found in DB")
                return@launch
            }

            Log.d("TEST", "Missed conversion: Current status = ${reminder.status}")

            if (reminder.status != "Pending") {
                Log.d("REMINDER_RECEIVER", "Missed conversion: status is ${reminder.status}, skipping")
                return@launch
            }

            // ✅ NOW convert to Missed
            db.reminderDao().updateReminderStatus(id, "Missed")
            db.doseLogDao().insertLog(
                DoseLogEntity(
                    id = UUID.randomUUID().toString(),
                    planId = reminder.planId ?: "",
                    reminderId = reminder.id,
                    scheduledDate = RecurrenceHelper.getTodayString(),
                    scheduledTime = reminder.time ?: "",
                    status = "Missed"
                )
            )
            sendRefreshBroadcast(context)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val medicineName = reminder.title

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("❌ Missed: $medicineName")
                .setContentText("You didn't take $medicineName. Status changed to Missed.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(id.hashCode() + 9999, notification)
            Log.d("REMINDER_RECEIVER", "Missed conversion completed for $id — status changed to Missed")
        }
    }

    private suspend fun handleSnooze(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"
        val originalTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Health Reminder"
        val originalDescription = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        Log.d("REMINDER_RECEIVER", "handleSnooze: id=$id, type=$type")

        dismissNotification(context, intent)

        val snoozeMinutes = SettingsPrefs.getReminderSnoozeMinutes(context)
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeMinutes)
        }
        val newHour = calendar.get(Calendar.HOUR_OF_DAY)
        val newMinute = calendar.get(Calendar.MINUTE)
        val newTime = String.format(Locale.getDefault(), "%02d:%02d", newHour, newMinute)
        val snoozeTriggerMillis = calendar.timeInMillis

        Log.d("REMINDER_RECEIVER", "New snooze time: $newTime (snoozeMinutes=$snoozeMinutes)")

        if (type == "alarm") {
            try {
                val db = AppDatabase.getDatabase(context)
                db.alarmDao().updateLastTriggeredStatus(id, "Snoozed")
            } catch (e: Exception) {
                Log.e("REMINDER_RECEIVER", "Failed to update Room alarm status", e)
            }
        } else {
            val db = AppDatabase.getDatabase(context)

            db.reminderDao().updateReminderStatus(id, "Snoozed")
            db.reminderDao().updateReminderTime(id, newTime)
            sendRefreshBroadcast(context)

            // Cancel old warning and missed alarms before scheduling new ones
            cancelWarningNotification(context, id)
            cancelMissedConversion(context, id)

            // Schedule snooze alarm (snoozeUsed is always true for this call path)
            scheduleReminderSnooze(context, id, snoozeTriggerMillis, originalTitle, originalDescription)

            Log.d("REMINDER_RECEIVER", "Reminder snoozed: $id at $newTime")
        }
    }

    private fun scheduleReminderSnooze(
        context: Context,
        reminderId: String,
        triggerAtMillis: Long,
        title: String,
        description: String
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
            putExtra(EXTRA_SNOOZE_USED, true)
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

        Log.d("REMINDER_RECEIVER", "Snooze alarm scheduled at $triggerAtMillis")
    }

    private fun scheduleWarningNotification(
        context: Context,
        reminderId: String,
        medicineName: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("REMINDER_RECEIVER", "Cannot schedule exact alarms for warning")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_WARNING
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, medicineName)
            putExtra(EXTRA_MISSED_MINUTES, NORMAL_WARNING_TO_MISSED_MINUTES)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + RC_WARNING_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("REMINDER_RECEIVER", "Warning alarm scheduled at $triggerAtMillis for $reminderId")
    }

    private fun scheduleMissedConversion(
        context: Context,
        reminderId: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("REMINDER_RECEIVER", "Cannot schedule exact alarms for missed conversion")
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

        Log.d("REMINDER_RECEIVER", "Missed conversion alarm scheduled at $triggerAtMillis for $reminderId")
    }

    private suspend fun handleMarkTaken(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "alarm"

        Log.d("REMINDER_RECEIVER", "handleMarkTaken: id=$id, type=$type")

        cancelWarningNotification(context, id)
        cancelMissedConversion(context, id)

        val db = AppDatabase.getDatabase(context)

        when (type) {
            "alarm" -> {
                db.alarmDao().updateLastTriggeredStatus(id, "Completed")
                AlarmHelper(context).cancelAlarm(id)
            }
            "reminder" -> {
                val reminder = db.reminderDao().getReminderById(id)

                if (reminder != null) {
                    // ✅ سجّل الجرعة دي في الـ Log عشان الإحصائيات
                    db.doseLogDao().insertLog(
                        DoseLogEntity(
                            id = UUID.randomUUID().toString(),
                            planId = reminder.planId ?: "",
                            reminderId = reminder.id,
                            scheduledDate = RecurrenceHelper.getTodayString(),
                            scheduledTime = reminder.time ?: "",
                            status = "Taken"
                        )
                    )
                }

                if (reminder != null && reminder.isRecurring && RecurrenceHelper.isRecurring(reminder.recurrenceType)) {
                    val nextMillis = RecurrenceHelper.computeNextTriggerMillis(
                        date = reminder.date,
                        time = reminder.time,
                        recurrenceType = reminder.recurrenceType
                    )

                    val reachedEnd = reminder.endDate != null && nextMillis != null &&
                            RecurrenceHelper.formatDate(nextMillis) > reminder.endDate!!

                    if (nextMillis != null && !reachedEnd) {
                        db.reminderDao().updateReminderStatus(id, "Pending")
                        ReminderScheduler.scheduleReminder(context, reminder, nextMillis)
                        Log.d("REMINDER_RECEIVER", "Recurring reminder marked taken, next alarm scheduled")
                    } else {
                        db.reminderDao().updateReminderStatus(id, "Completed")
                        Log.d("REMINDER_RECEIVER", "Reminder plan finished (reached end date)")
                    }
                } else {
                    db.reminderDao().updateReminderStatus(id, "Completed")
                    Log.d("REMINDER_RECEIVER", "Room reminder status updated to Completed")
                }
            }
        }

        sendRefreshBroadcast(context)
        dismissNotification(context, intent)
    }

    private fun sendRefreshBroadcast(context: Context) {
        val refreshIntent = Intent(ACTION_REFRESH_DASHBOARD)
        context.sendBroadcast(refreshIntent)
        Log.d("REMINDER_RECEIVER", "Refresh dashboard broadcast sent")
    }

    private fun cancelWarningNotification(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_WARNING
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + RC_WARNING_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("REMINDER_RECEIVER", "Cancelled warning alarm for $reminderId")
    }

    private fun cancelMissedConversion(context: Context, reminderId: String) {
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
        Log.d("REMINDER_RECEIVER", "Cancelled missed conversion alarm for $reminderId")
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id.hashCode())
        notificationManager.cancel(id.hashCode() + 7777) // also clear the Warning notification, if shown
        Log.d("REMINDER_RECEIVER", "Notification dismissed: $id")
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
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