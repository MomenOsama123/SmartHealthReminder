package com.example.smarthealthreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarthealthreminder.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val alarmId = intent.getStringExtra("alarm_id")
        if (alarmId.isNullOrBlank()) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isSnooze = intent.getBooleanExtra("is_snooze", false)
                val alarm = AppDatabase.getDatabase(context)
                    .alarmDao()
                    .getActiveAlarmById(alarmId)

                if (alarm == null && !isSnooze) {
                    return@launch
                }

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("alarm_label", alarm?.label ?: intent.getStringExtra("alarm_label"))
                    putExtra("alarm_time", intent.getStringExtra("alarm_time") ?: alarm?.time)
                    putExtra("alarm_category", alarm?.category ?: intent.getStringExtra("alarm_category"))
                    putExtra("is_snooze", isSnooze)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
