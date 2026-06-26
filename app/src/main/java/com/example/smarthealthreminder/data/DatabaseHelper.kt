package com.example.smarthealthreminder.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smarthealthreminder.features.model.Alarm
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.model_d.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, "health_sync.db", null, 6
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                firebase_id TEXT,
                name TEXT NOT NULL,
                email TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE alarms (
                id TEXT PRIMARY KEY,
                user_id INTEGER,
                label TEXT NOT NULL,
                time TEXT NOT NULL,
                am_pm TEXT,
                category TEXT,
                repeat_days TEXT,
                dosage TEXT,
                instructions TEXT,
                is_active INTEGER DEFAULT 1,
                status TEXT DEFAULT 'pending',
                sound TEXT,
                vibration_enabled INTEGER DEFAULT 0,
                gradual_volume INTEGER DEFAULT 0,
                auto_snooze_minutes INTEGER DEFAULT 0,
                cognitive_lock_enabled INTEGER DEFAULT 0
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE reminders (
                id TEXT PRIMARY KEY,
                user_id INTEGER DEFAULT 0,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT,
                date TEXT,
                time TEXT,
                priority TEXT,
                status TEXT DEFAULT 'Pending',
                is_recurring INTEGER DEFAULT 0,
                recurrence_type TEXT,
                vibration_enabled INTEGER DEFAULT 0,
                early_notification INTEGER DEFAULT 0,
                early_notification_minutes INTEGER DEFAULT 0
            )
        """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS alarms")
        db.execSQL("DROP TABLE IF EXISTS reminders")
        onCreate(db)
    }

    fun insertUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("firebase_id", user.firebaseId)
            put("name", user.name)
            put("email", user.email)
        }
        val id = db.insert("users", null, values)
        db.close()
        return id
    }

    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(userId.toString()))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(0),
                firebaseId = cursor.getString(1) ?: "",
                name = cursor.getString(2),
                email = cursor.getString(3)
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getUserByFirebaseId(firebaseId: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE firebase_id = ?",
            arrayOf(firebaseId)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(0),
                firebaseId = cursor.getString(1) ?: "",
                name = cursor.getString(2),
                email = cursor.getString(3)
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getAlarmsByUserId(userId: Int): List<Alarm> {
        val alarms = mutableListOf<Alarm>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM alarms WHERE user_id = ? AND is_active = 1 ORDER BY time ASC",
            arrayOf(userId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                alarms.add(
                    Alarm(
                        id = cursor.getString(0),
                        userId = cursor.getInt(1),
                        label = cursor.getString(2),
                        time = cursor.getString(3),
                        amPm = cursor.getString(4),
                        category = cursor.getString(5),
                        repeatDays = cursor.getString(6),
                        dosage = cursor.getString(7),
                        instructions = cursor.getString(8),
                        isActive = cursor.getInt(9) == 1,
                        status = cursor.getString(10)?.uppercase() ?: "PENDING",
                        sound = cursor.getString(11),
                        vibrationEnabled = cursor.getInt(12) == 1,
                        gradualVolume = cursor.getInt(13) == 1,
                        autoSnoozeMinutes = cursor.getInt(14),
                        cognitiveLockEnabled = cursor.getInt(15) == 1
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return alarms
    }

    fun getRemindersByUserId(userId: Int): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM reminders WHERE user_id = ? ORDER BY date, time ASC",
            arrayOf(userId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                reminders.add(
                    Reminder(
                        id = cursor.getString(0),
                        userId = cursor.getInt(1),
                        title = cursor.getString(2),
                        description = cursor.getString(3),
                        category = cursor.getString(4),
                        date = cursor.getString(5),
                        time = cursor.getString(6),
                        priority = cursor.getString(7),
                        status = cursor.getString(8),
                        isRecurring = cursor.getInt(9) == 1,
                        recurrenceType = cursor.getString(10),
                        vibrationEnabled = cursor.getInt(11) == 1,
                        earlyNotification = cursor.getInt(12) == 1,
                        earlyNotificationMinutes = cursor.getInt(13)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return reminders
    }

    fun markAlarmAsTaken(id: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "taken")
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    fun updateReminderStatus(id: String, status: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", status)
        }
        val rows = db.update("reminders", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    fun getAlarmById(id: String): Alarm? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM alarms WHERE id = ?", arrayOf(id))
        var alarm: Alarm? = null
        if (cursor.moveToFirst()) {
            alarm = Alarm(
                id = cursor.getString(0),
                userId = cursor.getInt(1),
                label = cursor.getString(2),
                time = cursor.getString(3),
                amPm = cursor.getString(4),
                category = cursor.getString(5),
                repeatDays = cursor.getString(6),
                dosage = cursor.getString(7),
                instructions = cursor.getString(8),
                isActive = cursor.getInt(9) == 1,
                status = cursor.getString(10)?.uppercase() ?: "PENDING",
                sound = cursor.getString(11),
                vibrationEnabled = cursor.getInt(12) == 1,
                gradualVolume = cursor.getInt(13) == 1,
                autoSnoozeMinutes = cursor.getInt(14),
                cognitiveLockEnabled = cursor.getInt(15) == 1
            )
        }
        cursor.close()
        db.close()
        return alarm
    }

    fun snoozeAlarmByTime(id: String, newTime: String, newAmPm: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("time", newTime)
            put("am_pm", newAmPm)
            put("status", "SNOOZED")
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }
}
