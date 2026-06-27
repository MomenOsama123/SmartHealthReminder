package com.example.smarthealthreminder.alarm

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

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAlarmsAfterBoot(context)
                    rescheduleReminders(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val alarmId = intent.getStringExtra(AlarmHelper.EXTRA_ALARM_ID)
            ?: intent.getStringExtra("alarm_id")
        if (alarmId.isNullOrBlank()) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarmTrigger(context, intent, alarmId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarmTrigger(context: Context, intent: Intent, alarmId: String) {
        val isSnooze = intent.getBooleanExtra(AlarmHelper.EXTRA_IS_SNOOZE, false)
            || intent.getBooleanExtra("is_snooze", false)
        val repeatDay = intent.getIntExtra(AlarmHelper.EXTRA_REPEAT_DAY, -1)

        val alarmEntity = AppDatabase.getDatabase(context)
            .alarmDao()
            .getActiveAlarmById(alarmId)

        if (alarmEntity == null) {
            if (!isSnooze) {
                AlarmHelper(context).cancelAllPendingIntents(alarmId)
                Log.d(TAG, "Cleaned orphan PendingIntents for missing alarm: $alarmId")
            }
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmHelper.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmHelper.EXTRA_ALARM_LABEL, alarmEntity.label)
            putExtra(
                AlarmHelper.EXTRA_ALARM_TIME,
                intent.getStringExtra(AlarmHelper.EXTRA_ALARM_TIME)
                    ?: AlarmHelper.formatDisplayTime(alarmEntity.time, alarmEntity.amPm)
            )
            putExtra(AlarmHelper.EXTRA_ALARM_CATEGORY, alarmEntity.category)
            putExtra(AlarmHelper.EXTRA_IS_SNOOZE, isSnooze)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        if (repeatDay != -1 && !isSnooze) {
            val alarm = Alarm(
                id = alarmEntity.id,
                label = alarmEntity.label,
                time = alarmEntity.time,
                amPm = alarmEntity.amPm,
                category = alarmEntity.category,
                isActive = alarmEntity.isActive,
                repeatDays = alarmEntity.repeatDays
            )
            AlarmHelper(context).rescheduleWeeklyOccurrence(alarm, repeatDay)
            Log.d(TAG, "Rescheduled weekly alarm ${alarm.id} for day $repeatDay")
        }
    }

    private suspend fun rescheduleAlarmsAfterBoot(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val alarmHelper = AlarmHelper(context)

            if (!alarmHelper.canScheduleExactAlarm()) {
                Log.w(TAG, "Cannot reschedule alarms after boot — exact alarm permission missing")
                return
            }

            val alarms = db.alarmDao().getAllAlarms().first()
            alarms.filter { it.isActive }.forEach { entity ->
                val stillExists = db.alarmDao().getAlarmById(entity.id)
                if (stillExists == null || !stillExists.isActive) {
                    alarmHelper.cancelAllPendingIntents(entity.id)
                    return@forEach
                }

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
                Log.d(TAG, "Restored alarm after boot: ${alarm.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarms after boot", e)
        }
    }

    private suspend fun rescheduleReminders(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val reminders = db.reminderDao().getAllReminders().first()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            val now = System.currentTimeMillis()

            reminders.filter { it.status == "Pending" || it.status == "Snoozed" }.forEach { reminder ->
                try {
                    val dateParts = reminder.date?.split("-") ?: return@forEach
                    val timeParts = reminder.time?.split(":") ?: return@forEach
                    if (dateParts.size < 3 || timeParts.size < 2) return@forEach

                    val calendar = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, dateParts[0].toInt())
                        set(java.util.Calendar.MONTH, dateParts[1].toInt() - 1)
                        set(java.util.Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                        set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }

                    if (calendar.timeInMillis < now) {
                        db.reminderDao().updateReminderStatus(reminder.id, "Missed")
                        Log.d(TAG, "Marked past reminder as missed: ${reminder.id}")
                        return@forEach
                    }

                    val intent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                        putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                        putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description ?: "Time for your health reminder!")
                        putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                        putExtra(ReminderReceiver.EXTRA_VIBRATION, reminder.vibrationEnabled)
                    }

                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        reminder.id.hashCode(),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Log.e(TAG, "Cannot schedule exact alarms for reminder: ${reminder.id}")
                        return@forEach
                    }

                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Rescheduled reminder: ${reminder.id} at ${reminder.date} ${reminder.time}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule reminder: ${reminder.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
