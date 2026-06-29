package com.example.smarthealthreminder.data.repository

import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.dao.*
import com.example.smarthealthreminder.features.data.local.entity.*
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var alarmDao: AlarmDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var calendarNoteDao: CalendarNoteDao
    private lateinit var scheduleEntryDao: ScheduleEntryDao
    private lateinit var reportDao: ReportDao
    private lateinit var repository: HealthRepository

    private val alarm = AlarmEntity(
        id = "a1", label = "Morning Meds", time = "08:00",
        amPm = "AM", category = "MEDICINE", isActive = true
    )
    private val reminder = ReminderEntity(
        id = "r1", title = "Take Aspirin", status = "Pending",
        date = "2025-06-22", time = "08:00"
    )
    private val scheduleEntry = ScheduleEntryEntity(
        id = "se1", title = "Doctor Visit", date = "2025-06-22", time = "10:00"
    )
    private val report = ReportEntity(
        id = "rep1",
        title = "Weekly Report",
        adherencePercentage = 90,
        missedDoses = 1,
        symptomsOverview = "No symptoms",
        aiInsight1 = "Excellent",
        aiInsight2 = "Continue",
        date = "2025-06-22"
    )
    private val note = CalendarNoteEntity(date = "2025-06-22", note = "Drink water")

    @Before
    fun setup() {
        alarmDao = mockk(relaxed = true)
        reminderDao = mockk(relaxed = true)
        calendarNoteDao = mockk(relaxed = true)
        scheduleEntryDao = mockk(relaxed = true)
        reportDao = mockk(relaxed = true)
        database = mockk()

        every { database.alarmDao() } returns alarmDao
        every { database.reminderDao() } returns reminderDao
        every { database.calendarNoteDao() } returns calendarNoteDao
        every { database.scheduleEntryDao() } returns scheduleEntryDao
        every { database.reportDao() } returns reportDao

        repository = HealthRepository(database)
    }

    // ───── Alarm Operations ─────

    @Test
    fun `getAllAlarms delegates to alarmDao`() = runTest {
        every { alarmDao.getAllAlarms() } returns flowOf(listOf(alarm))
        val result = repository.getAllAlarms().first()
        assertEquals(listOf(alarm), result)
    }

    @Test
    fun `insertAlarm delegates to alarmDao`() = runTest {
        repository.insertAlarm(alarm)
        coVerify { alarmDao.insertAlarm(alarm) }
    }

    @Test
    fun `updateAlarm delegates to alarmDao`() = runTest {
        repository.updateAlarm(alarm)
        coVerify { alarmDao.updateAlarm(alarm) }
    }

    @Test
    fun `deleteAlarm delegates to alarmDao`() = runTest {
        repository.deleteAlarm(alarm)
        coVerify { alarmDao.deleteAlarm(alarm) }
    }

    @Test
    fun `deleteAlarmById delegates to alarmDao`() = runTest {
        repository.deleteAlarmById("a1")
        coVerify { alarmDao.deleteAlarmById("a1") }
    }

    @Test
    fun `toggleAlarmStatus delegates to alarmDao`() = runTest {
        repository.toggleAlarmStatus("a1", false)
        coVerify { alarmDao.updateAlarmStatus("a1", false) }
    }

    @Test
    fun `searchAlarms adds wildcard and delegates`() = runTest {
        every { alarmDao.searchAlarms("%morning%") } returns flowOf(listOf(alarm))
        val result = repository.searchAlarms("morning").first()
        assertEquals(listOf(alarm), result)
    }

    // ───── Reminder Operations ─────

    @Test
    fun `getAllReminders delegates to reminderDao`() = runTest {
        every { reminderDao.getAllReminders() } returns flowOf(listOf(reminder))
        val result = repository.getAllReminders().first()
        assertEquals(listOf(reminder), result)
    }

    @Test
    fun `getRemindersByStatus delegates to reminderDao`() = runTest {
        every { reminderDao.getRemindersByStatus("Pending") } returns flowOf(listOf(reminder))
        val result = repository.getRemindersByStatus("Pending").first()
        assertEquals(listOf(reminder), result)
    }

    @Test
    fun `insertReminder delegates to reminderDao`() = runTest {
        repository.insertReminder(reminder)
        coVerify { reminderDao.insertReminder(reminder) }
    }

    @Test
    fun `updateReminder delegates to reminderDao`() = runTest {
        repository.updateReminder(reminder)
        coVerify { reminderDao.updateReminder(reminder) }
    }

    @Test
    fun `deleteReminder delegates to reminderDao`() = runTest {
        repository.deleteReminder(reminder)
        coVerify { reminderDao.deleteReminder(reminder) }
    }

    @Test
    fun `deleteReminderById delegates to reminderDao`() = runTest {
        repository.deleteReminderById("r1")
        coVerify { reminderDao.deleteReminderById("r1") }
    }

    @Test
    fun `markReminderDone updates status to Completed`() = runTest {
        repository.markReminderDone("r1")
        coVerify { reminderDao.updateReminderStatus("r1", "Completed") }
    }

    @Test
    fun `markReminderMissed updates status to Missed`() = runTest {
        repository.markReminderMissed("r1")
        coVerify { reminderDao.updateReminderStatus("r1", "Missed") }
    }

    @Test
    fun `searchReminders adds wildcard and delegates`() = runTest {
        every { reminderDao.searchReminders("%aspirin%") } returns flowOf(listOf(reminder))
        val result = repository.searchReminders("aspirin").first()
        assertEquals(listOf(reminder), result)
    }

    @Test
    fun `getPendingCount delegates to reminderDao`() = runTest {
        every { reminderDao.getPendingCount() } returns flowOf(3)
        val result = repository.getPendingCount().first()
        assertEquals(3, result)
    }

    @Test
    fun `getCompletedCount delegates to reminderDao`() = runTest {
        every { reminderDao.getCompletedCount() } returns flowOf(2)
        val result = repository.getCompletedCount().first()
        assertEquals(2, result)
    }

    @Test
    fun `getMissedCount delegates to reminderDao`() = runTest {
        every { reminderDao.getMissedCount() } returns flowOf(1)
        val result = repository.getMissedCount().first()
        assertEquals(1, result)
    }

    // ───── Schedule Entries ─────

    @Test
    fun `getAllScheduleEntries delegates to scheduleEntryDao`() = runTest {
        every { scheduleEntryDao.getAllEntries() } returns flowOf(listOf(scheduleEntry))
        val result = repository.getAllScheduleEntries().first()
        assertEquals(listOf(scheduleEntry), result)
    }

    @Test
    fun `insertScheduleEntry delegates to scheduleEntryDao`() = runTest {
        repository.insertScheduleEntry(scheduleEntry)
        coVerify { scheduleEntryDao.insertEntry(scheduleEntry) }
    }

    @Test
    fun `deleteScheduleEntryById delegates to scheduleEntryDao`() = runTest {
        repository.deleteScheduleEntryById("se1")
        coVerify { scheduleEntryDao.deleteEntryById("se1") }
    }

    // ───── Reports ─────

    @Test
    fun `getAllReports delegates to reportDao`() = runTest {
        every { reportDao.getAllReports() } returns flowOf(listOf(report))
        val result = repository.getAllReports().first()
        assertEquals(listOf(report), result)
    }

    @Test
    fun `insertReport delegates to reportDao`() = runTest {
        repository.insertReport(report)
        coVerify { reportDao.insertReport(report) }
    }

    @Test
    fun `deleteReportById delegates to reportDao`() = runTest {
        repository.deleteReportById("rep1")
        coVerify { reportDao.deleteReportById("rep1") }
    }

    // ───── Calendar Notes ─────

    @Test
    fun `getNoteByDate delegates to calendarNoteDao`() = runTest {
        coEvery { calendarNoteDao.getNoteByDate("2025-06-22") } returns note
        val result = repository.getNoteByDate("2025-06-22")
        assertEquals(note, result)
    }

    @Test
    fun `saveNote creates entity and upserts`() = runTest {
        repository.saveNote("2025-06-22", "Drink water")
        coVerify { calendarNoteDao.upsertNote(CalendarNoteEntity("2025-06-22", "Drink water")) }
    }

    @Test
    fun `deleteNote delegates to calendarNoteDao`() = runTest {
        repository.deleteNote("2025-06-22")
        coVerify { calendarNoteDao.deleteNoteByDate("2025-06-22") }
    }

    @Test
    fun `getAllNoteDates delegates to calendarNoteDao`() = runTest {
        every { calendarNoteDao.getAllNoteDates() } returns flowOf(listOf("2025-06-22"))
        val result = repository.getAllNoteDates().first()
        assertEquals(listOf("2025-06-22"), result)
    }
}
