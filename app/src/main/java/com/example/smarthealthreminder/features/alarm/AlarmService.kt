package com.example.smarthealthreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.AlarmRingingActivity
import com.example.smarthealthreminder.features.settings.SettingsActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isRunning = false  // ← عشان نمنع تشغيل الصوت أكتر من مرة

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // لو جه أمر stop — وقف كل حاجة فوراً
        if (intent?.action == ACTION_STOP) {
            stopAlarmSound()
            stopVibration()
            stopForeground(true)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        // لو الصوت شغال بالفعل، متشغلوش تاني
        if (isRunning) return START_NOT_STICKY
        isRunning = true

        val alarmId = intent?.getStringExtra("alarm_id")
        if (alarmId.isNullOrBlank()) {
            stopForeground(true)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        val label = intent.getStringExtra("alarm_label") ?: "Alarm"
        val alarmTime = intent?.getStringExtra("alarm_time")
        val alarmCategory = intent?.getStringExtra("alarm_category")

        startForeground(NOTIFICATION_ID, buildNotification(label, alarmId, alarmTime, alarmCategory))
        playAlarmSound()
        startVibration()

        // افتح شاشة الـ alarm
        val alarmIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_TIME, alarmTime)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_CATEGORY, alarmCategory)
        }
        startActivity(alarmIntent)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
        stopForeground(true)
        isRunning = false
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        label: String,
        alarmId: String,
        alarmTime: String?,
        alarmCategory: String?
    ): Notification {
        val intent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_TIME, alarmTime)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_CATEGORY, alarmCategory)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Alarm Ringing!")
            .setContentText("$label - Tap to dismiss")
            .setSmallIcon(R.drawable.ic_bell)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun playAlarmSound() {
        try {
            mediaPlayer = MediaPlayer.create(
                this,
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            )
            mediaPlayer?.apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        val vibrationEnabled = getSharedPreferences(
            SettingsActivity.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(SettingsActivity.KEY_VIBRATION, true)

        if (!vibrationEnabled) return

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500, 500, 500, 500), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }
}
