package com.example.smarthealthreminder.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smarthealthreminder.features.model.Alarm
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.model_d.User
import com.example.smarthealthreminder.model_d.Medication
import com.example.smarthealthreminder.model_d.DailyDose
import com.example.smarthealthreminder.model_d.DoseStatus
import com.example.smarthealthreminder.model_d.HealthMetric
import com.example.smarthealthreminder.model_d.MetricType
import com.example.smarthealthreminder.model_d.MedicationStats
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, "health_sync.db", null, 6
) {

    override fun onCreate(db: SQLiteDatabase) {
        // Users
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                firebase_id TEXT,
                name TEXT NOT NULL,
                email TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        // Medications
        db.execSQL("""
            CREATE TABLE medications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                name TEXT NOT NULL,
                dosage TEXT,
                time_of_day TEXT,
                instructions TEXT,
                icon_type TEXT DEFAULT 'pill',
                is_active INTEGER DEFAULT 1
            )
        """)

        // Daily Doses
        db.execSQL("""
            CREATE TABLE daily_doses (
                dose_id INTEGER PRIMARY KEY AUTOINCREMENT,
                med_id INTEGER,
                user_id INTEGER,
                scheduled_time TEXT,
                status TEXT DEFAULT 'pending',
                dose_date TEXT
            )
        """)

        // Health Metrics
        db.execSQL("""
            CREATE TABLE health_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                metric_type TEXT,
                value TEXT,
                unit TEXT,
                recorded_date TEXT
            )
        """)

        // Alarms
        db.execSQL("""
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
        """)

        // Reminders
        db.execSQL("""
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
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS medications")
        db.execSQL("DROP TABLE IF EXISTS daily_doses")
        db.execSQL("DROP TABLE IF EXISTS health_metrics")
        db.execSQL("DROP TABLE IF EXISTS alarms")
        db.execSQL("DROP TABLE IF EXISTS reminders")
        onCreate(db)
    }

    // ==================== USERS ====================

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
        val cursor = db.rawQuery("SELECT * FROM users WHERE firebase_id = ?", arrayOf(firebaseId))
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

    // ==================== MEDICATIONS ====================

    fun insertMedication(med: Medication): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", med.userId)
            put("name", med.name)
            put("dosage", med.dosage)
            put("time_of_day", med.timeOfDay)
            put("instructions", med.instructions)
            put("icon_type", med.iconType)
        }
        val medId = db.insert("medications", null, values)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until 7) {
            val doseDate = dateFormat.format(calendar.time)
            val doseValues = ContentValues().apply {
                put("med_id", medId)
                put("user_id", med.userId)
                put("scheduled_time", med.timeOfDay)
                put("status", "pending")
                put("dose_date", doseDate)
            }
            db.insert("daily_doses", null, doseValues)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        db.close()
        return medId
    }

    fun getMedicationsByUserId(userId: Int): List<Medication> {
        val medications = mutableListOf<Medication>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM medications WHERE user_id = ? AND is_active = 1",
            arrayOf(userId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                medications.add(Medication(
                    id = cursor.getInt(0),
                    userId = cursor.getInt(1),
                    name = cursor.getString(2),
                    dosage = cursor.getString(3),
                    timeOfDay = cursor.getString(4),
                    instructions = cursor.getString(5),
                    iconType = cursor.getString(6)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return medications
    }

    // ==================== DAILY DOSES ====================

    fun getDailyDosesByDate(userId: Int, date: String): List<DailyDose> {
        val doses = mutableListOf<DailyDose>()
        val db = readableDatabase
        val query = """
            SELECT d.*, m.name, m.dosage, m.instructions, m.icon_type 
            FROM daily_doses d
            INNER JOIN medications m ON d.med_id = m.id
            WHERE d.user_id = ? AND d.dose_date = ? AND m.is_active = 1
            ORDER BY d.scheduled_time ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), date))
        if (cursor.moveToFirst()) {
            do {
                doses.add(DailyDose(
                    doseId = cursor.getInt(0),
                    medId = cursor.getInt(1),
                    userId = cursor.getInt(2),
                    medicationName = cursor.getString(6),
                    dosage = cursor.getString(7),
                    instructions = cursor.getString(8),
                    iconType = cursor.getString(9),
                    scheduledTime = cursor.getString(3),
                    takenAt = null,
                    status = DoseStatus.valueOf(cursor.getString(4).uppercase()),
                    doseDate = cursor.getString(5)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return doses
    }

    fun markDoseAsTaken(doseId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "taken")
        }
        val rows = db.update("daily_doses", values, "dose_id = ?", arrayOf(doseId.toString()))
        db.close()
        return rows > 0
    }

    fun snoozeDose(doseId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "snoozed")
        }
        val rows = db.update("daily_doses", values, "dose_id = ?", arrayOf(doseId.toString()))
        db.close()
        return rows > 0
    }

    fun getNextUpcomingDose(userId: Int, date: String, time: String): DailyDose? {
        val db = readableDatabase
        val query = """
            SELECT d.*, m.name, m.dosage, m.instructions, m.icon_type 
            FROM daily_doses d
            INNER JOIN medications m ON d.med_id = m.id
            WHERE d.user_id = ? AND d.dose_date = ? AND d.status = 'pending'
            AND d.scheduled_time >= ? AND m.is_active = 1
            ORDER BY d.scheduled_time ASC LIMIT 1
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), date, time))
        var dose: DailyDose? = null
        if (cursor.moveToFirst()) {
            dose = DailyDose(
                doseId = cursor.getInt(0),
                medId = cursor.getInt(1),
                userId = cursor.getInt(2),
                medicationName = cursor.getString(6),
                dosage = cursor.getString(7),
                instructions = cursor.getString(8),
                iconType = cursor.getString(9),
                scheduledTime = cursor.getString(3),
                takenAt = null,
                status = DoseStatus.PENDING,
                doseDate = cursor.getString(5)
            )
        }
        cursor.close()
        db.close()
        return dose
    }

    // ==================== STATS ====================

    fun getMedicationStats(userId: Int, date: String): MedicationStats {
        val db = readableDatabase
        val totalCursor = db.rawQuery(
            "SELECT COUNT(*) FROM daily_doses WHERE user_id = ? AND dose_date = ?",
            arrayOf(userId.toString(), date)
        )
        val takenCursor = db.rawQuery(
            "SELECT COUNT(*) FROM daily_doses WHERE user_id = ? AND dose_date = ? AND status = 'taken'",
            arrayOf(userId.toString(), date)
        )
        val missedCursor = db.rawQuery(
            "SELECT COUNT(*) FROM daily_doses WHERE user_id = ? AND dose_date = ? AND status = 'missed'",
            arrayOf(userId.toString(), date)
        )
        var total = 0
        var taken = 0
        var missed = 0
        if (totalCursor.moveToFirst()) total = totalCursor.getInt(0)
        if (takenCursor.moveToFirst()) taken = takenCursor.getInt(0)
        if (missedCursor.moveToFirst()) missed = missedCursor.getInt(0)
        totalCursor.close()
        takenCursor.close()
        missedCursor.close()
        db.close()
        val percentage = if (total > 0) (taken * 100 / total) else 0
        return MedicationStats(total, taken, missed, percentage)
    }

    // ==================== HEALTH METRICS ====================

    fun insertHealthMetric(metric: HealthMetric): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", metric.userId)
            put("metric_type", metric.type.name.lowercase())
            put("value", metric.value)
            put("unit", metric.unit)
            put("recorded_date", metric.date)
        }
        val id = db.insert("health_metrics", null, values)
        db.close()
        return id
    }

    fun getLatestHealthMetric(userId: Int, type: MetricType): HealthMetric? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM health_metrics WHERE user_id = ? AND metric_type = ? ORDER BY recorded_date DESC LIMIT 1",
            arrayOf(userId.toString(), type.name.lowercase())
        )
        var metric: HealthMetric? = null
        if (cursor.moveToFirst()) {
            metric = HealthMetric(
                id = cursor.getInt(0),
                userId = cursor.getInt(1),
                type = type,
                value = cursor.getString(3),
                unit = cursor.getString(4),
                date = cursor.getString(5)
            )
        }
        cursor.close()
        db.close()
        return metric
    }

    // ==================== ALARMS CRUD ====================

    fun insertAlarm(alarm: Alarm): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", alarm.id)
            put("user_id", alarm.userId)
            put("label", alarm.label)
            put("time", alarm.time)
            put("am_pm", alarm.amPm)
            put("category", alarm.category)
            put("repeat_days", alarm.repeatDays)
            put("dosage", alarm.dosage)
            put("instructions", alarm.instructions)
            put("is_active", if (alarm.isActive) 1 else 0)
            put("status", alarm.status.lowercase())
            put("sound", alarm.sound)
            put("vibration_enabled", if (alarm.vibrationEnabled) 1 else 0)
            put("gradual_volume", if (alarm.gradualVolume) 1 else 0)
            put("auto_snooze_minutes", alarm.autoSnoozeMinutes)
            put("cognitive_lock_enabled", if (alarm.cognitiveLockEnabled) 1 else 0)
        }
        val result = db.insert("alarms", null, values)
        db.close()
        return result
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

    fun deleteAlarm(id: String): Boolean {
        val db = writableDatabase
        val rows = db.delete("alarms", "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    fun updateAlarmActive(id: String, isActive: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_active", if (isActive) 1 else 0)
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
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

    fun markAlarmAsMissed(id: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "missed")
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    fun snoozeAlarmStatus(id: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "snoozed")
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    fun snoozeAlarmByTime(id: String, newTime: String, newAmPm: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("time", newTime)
            put("am_pm", newAmPm)
            put("status", "pending")
        }
        val rows = db.update("alarms", values, "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    // ==================== REMINDERS CRUD ====================

    fun insertReminder(reminder: Reminder): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", reminder.id)
            put("user_id", reminder.userId)
            put("title", reminder.title)
            put("description", reminder.description)
            put("category", reminder.category)
            put("date", reminder.date)
            put("time", reminder.time)
            put("priority", reminder.priority)
            put("status", reminder.status)
            put("is_recurring", if (reminder.isRecurring) 1 else 0)
            put("recurrence_type", reminder.recurrenceType)
            put("vibration_enabled", if (reminder.vibrationEnabled) 1 else 0)
            put("early_notification", if (reminder.earlyNotification) 1 else 0)
            put("early_notification_minutes", reminder.earlyNotificationMinutes)
        }
        val result = db.insert("reminders", null, values)
        db.close()
        return result
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
                reminders.add(Reminder(
                    id = cursor.getString(0),
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
                ).apply { this.userId = cursor.getInt(1) })
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return reminders
    }

    fun updateReminder(reminder: Reminder): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", reminder.userId)
            put("title", reminder.title)
            put("description", reminder.description)
            put("category", reminder.category)
            put("date", reminder.date)
            put("time", reminder.time)
            put("priority", reminder.priority)
            put("status", reminder.status)
            put("is_recurring", if (reminder.isRecurring) 1 else 0)
            put("recurrence_type", reminder.recurrenceType)
            put("vibration_enabled", if (reminder.vibrationEnabled) 1 else 0)
            put("early_notification", if (reminder.earlyNotification) 1 else 0)
            put("early_notification_minutes", reminder.earlyNotificationMinutes)
        }
        val rows = db.update("reminders", values, "id = ?", arrayOf(reminder.id))
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

    fun deleteReminder(id: String): Boolean {
        val db = writableDatabase
        val rows = db.delete("reminders", "id = ?", arrayOf(id))
        db.close()
        return rows > 0
    }

    // ==================== UTILS ====================

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun closeDatabase() {
        this.close()
    }
}