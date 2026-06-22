package com.example.smarthealthreminder.data.repository

import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.local.dao.AlarmDao
import com.example.smarthealthreminder.data.local.dao.CalendarNoteDao
import com.example.smarthealthreminder.data.local.dao.ReminderDao
import com.example.smarthealthreminder.data.local.dao.ReportDao
import com.example.smarthealthreminder.data.local.dao.ScheduleEntryDao
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.data.local.entity.ReportEntity
import com.example.smarthealthreminder.data.local.entity.ScheduleEntryEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var alarmDao: AlarmDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var calendarNoteDao: CalendarNoteDao
    private lateinit var scheduleEntryDao: ScheduleEntryDao
    private lateinit var reportDao: ReportDao
    private lateinit var repository: HealthRepository

    @Before
    fun setup() {
        alarmDao = mockk(relaxed = true)
        reminderDao = mockk(relaxed = true)
        calendarNoteDao = mockk(relaxed = true)
        scheduleEntryDao = mockk(relaxed = true)
        reportDao = mockk(relaxed = true)
        database = mockk(relaxed = true)

        every { database.alarmDao() } returns alarmDao
        every { database.reminderDao() } returns reminderDao
        every { database.calendarNoteDao() } returns calendarNoteDao
        every { database.scheduleEntryDao() } returns scheduleEntryDao
        every { database.reportDao() } returns reportDao

        repository = HealthRepository(database)
    }

    @Test
    fun `getAllAlarms returns flow from dao`() = runBlocking {
        val alarms = listOf(AlarmEntity("1", "Alarm", "08:00", "AM", "MEDICINE"))
        every { alarmDao.getAllAlarms() } returns flowOf(alarms)
        assertEquals(alarms, repository.getAllAlarms().first())
    }

    @Test
    fun `insertAlarm delegates to dao`() = runBlocking {
        val alarm = AlarmEntity("1", "Alarm", "08:00", "AM", "MEDICINE")
        repository.insertAlarm(alarm)
        coVerify { alarmDao.insertAlarm(alarm) }
    }

    @Test
    fun `insertReminder delegates to dao`() = runBlocking {
        val reminder = ReminderEntity("1", "Test", status = "Pending")
        repository.insertReminder(reminder)
        coVerify { reminderDao.insertReminder(reminder) }
    }

    @Test
    fun `markReminderDone delegates to dao`() = runBlocking {
        repository.markReminderDone("1")
        coVerify { reminderDao.updateReminderStatus("1", "Completed") }
    }

    @Test
    fun `saveNote delegates to dao`() = runBlocking {
        repository.saveNote("2024-01-01", "Note")
        coVerify { calendarNoteDao.upsertNote(CalendarNoteEntity("2024-01-01", "Note")) }
    }

    @Test
    fun `insertScheduleEntry delegates to dao`() = runBlocking {
        val entry = ScheduleEntryEntity("1", "Entry", date = "2024-01-01")
        repository.insertScheduleEntry(entry)
        coVerify { scheduleEntryDao.insertEntry(entry) }
    }

    @Test
    fun `insertReport delegates to dao`() = runBlocking {
        val report = ReportEntity("1", "Report", date = "2024-01-01")
        repository.insertReport(report)
        coVerify { reportDao.insertReport(report) }
    }

    @Test
    fun `getAllReports returns flow from dao`() = runBlocking {
        val reports = listOf(ReportEntity("1", "Report", date = "2024-01-01"))
        every { reportDao.getAllReports() } returns flowOf(reports)
        assertEquals(reports, repository.getAllReports().first())
    }

    @Test
    fun `getAllScheduleEntries returns flow from dao`() = runBlocking {
        val entries = listOf(ScheduleEntryEntity("1", "Entry", date = "2024-01-01"))
        every { scheduleEntryDao.getAllEntries() } returns flowOf(entries)
        assertEquals(entries, repository.getAllScheduleEntries().first())
    }
}
