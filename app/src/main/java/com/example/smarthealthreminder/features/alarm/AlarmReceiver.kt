package com.example.smarthealthreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smarthealthreminder.features.activity.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id")
        val alarmLabel = intent.getStringExtra("alarm_label")
        val alarmTime = intent.getStringExtra("alarm_time")
        val alarmCategory = intent.getStringExtra("alarm_category")
        val isSnooze = intent.getBooleanExtra("is_snooze", false)

        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_TIME, alarmTime)
            putExtra(AlarmRingingActivity.EXTRA_ALARM_CATEGORY, alarmCategory)
            putExtra("is_snooze", isSnooze)
        }
        context.startActivity(alarmIntent)
    }
}
