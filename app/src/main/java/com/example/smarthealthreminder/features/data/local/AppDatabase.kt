package com.example.smarthealthreminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smarthealthreminder.data.local.dao.AlarmDao
import com.example.smarthealthreminder.data.local.dao.ReminderDao
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity

@Database(
    entities = [AlarmEntity::class, ReminderEntity::class],
    version = 2, // تم التحديث بنجاح لمنع الكراش بعد تعديل الـ Entity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smart_health_db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}