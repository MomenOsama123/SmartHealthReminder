package com.example.smarthealthreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id")
        val alarmLabel = intent.getStringExtra("alarm_label")
        val alarmTime = intent.getStringExtra("alarm_time")
        val alarmCategory = intent.getStringExtra("alarm_category")
        val isSnooze = intent.getBooleanExtra("is_snooze", false)

        // ← شغلي الـ Service بدل ما تفتحي Activity مباشرة
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_label", alarmLabel)
            putExtra("alarm_time", alarmTime)
            putExtra("alarm_category", alarmCategory)
            putExtra("is_snooze", isSnooze)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}