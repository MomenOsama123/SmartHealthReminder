//// app/src/main/java/com/example/smarthealthreminder/alarm/AlarmHelper.kt
//package com.example.smarthealthreminder.alarm
//
//import android.app.AlarmManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import com.example.smarthealthreminder.model.Alarm
//import java.util.Calendar
//
//class AlarmHelper(private val context: Context) {
//
//    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//    fun scheduleAlarm(alarm: Alarm) {
//        val intent = Intent(context, AlarmReceiver::class.java).apply {
//            putExtra("alarm_id", alarm.id)
//            putExtra("alarm_label", alarm.label)
//        }
//
//        val pendingIntent = PendingIntent.getBroadcast(
//            context,
//            alarm.id?.hashCode() ?: 0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val calendar = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, alarm.time?.split(":")?.get(0)?.toInt() ?: 8)
//            set(Calendar.MINUTE, alarm.time?.split(":")?.get(1)?.toInt() ?: 0)
//            set(Calendar.SECOND, 0)
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (alarmManager.canScheduleExactAlarms()) {
//                alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP,
//                    calendar.timeInMillis,
//                    pendingIntent
//                )
//            }
//        } else {
//            alarmManager.setExactAndAllowWhileIdle(
//                AlarmManager.RTC_WAKEUP,
//                calendar.timeInMillis,
//                pendingIntent
//            )
//        }
//    }
//
//    fun cancelAlarm(alarm: Alarm) {
//        val intent = Intent(context, AlarmReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context,
//            alarm.id?.hashCode() ?: 0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.cancel(pendingIntent)
//    }
//
//    fun snoozeAlarm(alarm: Alarm, minutes: Int = 10) {
//        val intent = Intent(context, AlarmReceiver::class.java).apply {
//            putExtra("alarm_id", alarm.id)
//            putExtra("alarm_label", alarm.label)
//            putExtra("is_snooze", true)
//        }
//
//        val pendingIntent = PendingIntent.getBroadcast(
//            context,
//            (alarm.id ?: "0").hashCode() + 1000,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000)
//
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            snoozeTime,
//            pendingIntent
//        )
//    }
//}