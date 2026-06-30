package com.example.smarthealthreminder.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.dao.*
import com.example.smarthealthreminder.features.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var alarmDao: AlarmDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var calendarNoteDao: CalendarNoteDao
    private lateinit var scheduleEntryDao: ScheduleEntryDao
    private lateinit var reportDao: ReportDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alarmDao = database.alarmDao()
        reminderDao = database.reminderDao()
        calendarNoteDao = database.calendarNoteDao()
        scheduleEntryDao = database.scheduleEntryDao()
        reportDao = database.reportDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve alarm`() = runBlocking {
        val alarm = AlarmEntity("1", "Morning", "08:00", "AM", "MEDICINE", isActive = true)
        alarmDao.insertAlarm(alarm)
        val retrieved = alarmDao.getAlarmById("1")
        assertNotNull(retrieved)
        assertEquals("Morning", retrieved?.label)
    }

    @Test
    fun `insert and retrieve reminder`() = runBlocking {
        val reminder = ReminderEntity("1", "Medicine", "Take pill", "Health", "2024-01-01", "09:00", "High", "Pending")
        reminderDao.insertReminder(reminder)
        val retrieved = reminderDao.getReminderById("1")
        assertNotNull(retrieved)
        assertEquals("Medicine", retrieved?.title)
    }

    @Test
    fun `update reminder status`() = runBlocking {
        val reminder = ReminderEntity("1", "Medicine", status = "Pending")
        reminderDao.insertReminder(reminder)
        reminderDao.updateReminderStatus("1", "Completed")
        val retrieved = reminderDao.getReminderById("1")
        assertEquals("Completed", retrieved?.status)
    }

    @Test
    fun `insert and retrieve calendar note`() = runBlocking {
        val note = CalendarNoteEntity("2024-01-01", "Doctor appointment")
        calendarNoteDao.upsertNote(note)
        val retrieved = calendarNoteDao.getNoteByDate("2024-01-01")
        assertNotNull(retrieved)
        assertEquals("Doctor appointment", retrieved?.note)
    }

    @Test
    fun `delete calendar note`() = runBlocking {
        val note = CalendarNoteEntity("2024-01-01", "Doctor appointment")
        calendarNoteDao.upsertNote(note)
        calendarNoteDao.deleteNoteByDate("2024-01-01")
        val retrieved = calendarNoteDao.getNoteByDate("2024-01-01")
        assertNull(retrieved)
    }

    @Test
    fun `insert and retrieve schedule entry`() = runBlocking {
        val entry = ScheduleEntryEntity("1", "Meeting", "Team sync", "2024-01-01", "10:00", "Work")
        scheduleEntryDao.insertEntry(entry)
        val retrieved = scheduleEntryDao.getEntriesByDate("2024-01-01").first()
        assertEquals(1, retrieved.size)
        assertEquals("Meeting", retrieved[0].title)
    }

    @Test
    fun `insert and retrieve report`() = runBlocking {
        val report = ReportEntity(
            id = "1",
            title = "Health Report",
            description = "Monthly summary",
            adherencePercentage = 85,
            missedDoses = 2,
            symptomsOverview = "Stable",
            aiInsight1 = "Good progress",
            aiInsight2 = "Stay hydrated",
            date = "2024-01-01",
        )
        reportDao.insertReport(report)
        val retrieved = reportDao.getReportsByDate("2024-01-01").first()
        assertEquals(1, retrieved.size)
        assertEquals("Health Report", retrieved[0].title)
    }

    @Test
    fun `getAllNoteDates returns dates with notes`() = runBlocking {
        calendarNoteDao.upsertNote(CalendarNoteEntity("2024-01-01", "Note 1"))
        calendarNoteDao.upsertNote(CalendarNoteEntity("2024-01-02", "Note 2"))
        val dates = calendarNoteDao.getAllNoteDates().first()
        assertEquals(2, dates.size)
    }

    @Test
    fun `getRemindersByDate returns correct reminders`() = runBlocking {
        val r1 = ReminderEntity("1", "A", date = "2024-01-01", status = "Pending")
        val r2 = ReminderEntity("2", "B", date = "2024-01-02", status = "Pending")
        reminderDao.insertReminder(r1)
        reminderDao.insertReminder(r2)
        val results = reminderDao.getRemindersByDate("2024-01-01").first()
        assertEquals(1, results.size)
        assertEquals("A", results[0].title)
    }
}