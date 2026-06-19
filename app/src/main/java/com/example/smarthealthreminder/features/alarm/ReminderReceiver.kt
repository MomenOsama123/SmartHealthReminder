package com.example.smarthealthreminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.settings.SettingsActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean(SettingsActivity.KEY_NOTIFICATIONS, true)) {
            return
        }

        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("reminder_title") ?: "Health Reminder"
        val description = intent.getStringExtra("reminder_description") ?: ""
        val vibrationEnabled = intent.getBooleanExtra("vibration_enabled", false) &&
            settings.getBoolean(SettingsActivity.KEY_VIBRATION, true)

        showNotification(context, reminderId, title, description)

        if (vibrationEnabled) {
            vibrate(context)
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: String,
        title: String,
        description: String
    ) {
        val channelId = "reminder_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Health Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for health reminders"
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)  // ← جوه الـ if
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("💊 $title")
            .setContentText(description.ifEmpty { "Time for your health reminder!" })
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
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
