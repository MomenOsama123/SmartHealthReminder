//// app/src/main/java/com/example/smarthealthreminder/alarm/AlarmReceiver.kt
//package com.example.smarthealthreminder.alarm
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import com.example.smarthealthreminder.activity.AlarmRingingActivity
//
//class AlarmReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        }
//        context.startActivity(alarmIntent)
//    }
//}