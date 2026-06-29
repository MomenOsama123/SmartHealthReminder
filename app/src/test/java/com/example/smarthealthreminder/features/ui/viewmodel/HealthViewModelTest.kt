package com.example.smarthealthreminder.features.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.features.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: HealthRepository
    private lateinit var viewModel: HealthViewModel

    private val sampleAlarm = AlarmEntity(
        id = "a1", label = "Morning Meds", time = "08:00",
        amPm = "AM", category = "MEDICINE", isActive = true
    )
    private val sampleReminder = ReminderEntity(
        id = "r1", title = "Take Aspirin", status = "Pending",
        date = "2025-06-22", time = "08:00", priority = "Medium"
    )
    private val sampleScheduleEntry = ScheduleEntryEntity(
        id = "se1", title = "Doctor Visit", date = "2025-06-22",
        time = "10:00", category = "Appointment"
    )
    private val sampleReport = ReportEntity(
        id = "rep1",
        title = "Weekly Report",
        adherencePercentage = 95,
        missedDoses = 0,
        symptomsOverview = "Healthy",
        aiInsight1 = "Keep it up",
        aiInsight2 = "Stay fit",
        date = "2025-06-22"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.getAllAlarms() } returns flowOf(listOf(sampleAlarm))
        every { repository.getAllReminders() } returns flowOf(listOf(sampleReminder))
        every { repository.getRemindersByStatus("Pending") } returns flowOf(listOf(sampleReminder))
        every { repository.getPendingCount() } returns flowOf(1)
        every { repository.getCompletedCount() } returns flowOf(0)
        every { repository.getMissedCount() } returns flowOf(0)
        every { repository.getAllNoteDates() } returns flowOf(listOf("2025-06-22"))
        every { repository.getAllScheduleEntries() } returns flowOf(listOf(sampleScheduleEntry))
        every { repository.getAllReports() } returns flowOf(listOf(sampleReport))

        viewModel = HealthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ───── Alarms ─────

    @Test
    fun `allAlarms emits list from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf(sampleAlarm), viewModel.allAlarms.value)
    }

    @Test
    fun `addAlarm calls repository insertAlarm`() = runTest {
        viewModel.addAlarm(sampleAlarm)
        advanceUntilIdle()
        coVerify { repository.insertAlarm(sampleAlarm) }
    }

    @Test
    fun `updateAlarm calls repository updateAlarm`() = runTest {
        viewModel.updateAlarm(sampleAlarm)
        advanceUntilIdle()
        coVerify { repository.updateAlarm(sampleAlarm) }
    }

    @Test
    fun `deleteAlarm calls repository deleteAlarm`() = runTest {
        viewModel.deleteAlarm(sampleAlarm)
        advanceUntilIdle()
        coVerify { repository.deleteAlarm(sampleAlarm) }
    }

    @Test
    fun `deleteAlarmById calls repository deleteAlarmById`() = runTest {
        viewModel.deleteAlarmById("a1")
        advanceUntilIdle()
        coVerify { repository.deleteAlarmById("a1") }
    }

    @Test
    fun `toggleAlarm calls repository toggleAlarmStatus`() = runTest {
        viewModel.toggleAlarm("a1", false)
        advanceUntilIdle()
        coVerify { repository.toggleAlarmStatus("a1", false) }
    }

    // ───── Reminders ─────

    @Test
    fun `allReminders emits list from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf(sampleReminder), viewModel.allReminders.value)
    }

    @Test
    fun `pendingReminders emits only pending items`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf(sampleReminder), viewModel.pendingReminders.value)
    }

    @Test
    fun `pendingCount emits correct count`() = runTest {
        advanceUntilIdle()
        assertEquals(1, viewModel.pendingCount.value)
    }

    @Test
    fun `completedCount emits correct count`() = runTest {
        advanceUntilIdle()
        assertEquals(0, viewModel.completedCount.value)
    }

    @Test
    fun `missedCount emits correct count`() = runTest {
        advanceUntilIdle()
        assertEquals(0, viewModel.missedCount.value)
    }

    @Test
    fun `addReminder calls repository insertReminder`() = runTest {
        viewModel.addReminder(sampleReminder)
        advanceUntilIdle()
        coVerify { repository.insertReminder(sampleReminder) }
    }

    @Test
    fun `updateReminder calls repository updateReminder`() = runTest {
        viewModel.updateReminder(sampleReminder)
        advanceUntilIdle()
        coVerify { repository.updateReminder(sampleReminder) }
    }

    @Test
    fun `deleteReminder calls repository deleteReminder`() = runTest {
        viewModel.deleteReminder(sampleReminder)
        advanceUntilIdle()
        coVerify { repository.deleteReminder(sampleReminder) }
    }

    @Test
    fun `deleteReminderById calls repository deleteReminderById`() = runTest {
        viewModel.deleteReminderById("r1")
        advanceUntilIdle()
        coVerify { repository.deleteReminderById("r1") }
    }

    @Test
    fun `markReminderDone calls repository markReminderDone`() = runTest {
        viewModel.markReminderDone("r1")
        advanceUntilIdle()
        coVerify { repository.markReminderDone("r1") }
    }

    @Test
    fun `markReminderMissed calls repository markReminderMissed`() = runTest {
        viewModel.markReminderMissed("r1")
        advanceUntilIdle()
        coVerify { repository.markReminderMissed("r1") }
    }

    // ───── Schedule Entries ─────

    @Test
    fun `allScheduleEntries emits list from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf(sampleScheduleEntry), viewModel.allScheduleEntries.value)
    }

    @Test
    fun `addScheduleEntry calls repository insertScheduleEntry`() = runTest {
        viewModel.addScheduleEntry(sampleScheduleEntry)
        advanceUntilIdle()
        coVerify { repository.insertScheduleEntry(sampleScheduleEntry) }
    }

    @Test
    fun `deleteScheduleEntryById calls repository deleteScheduleEntryById`() = runTest {
        viewModel.deleteScheduleEntryById("se1")
        advanceUntilIdle()
        coVerify { repository.deleteScheduleEntryById("se1") }
    }

    // ───── Reports ─────

    @Test
    fun `allReports emits list from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf(sampleReport), viewModel.allReports.value)
    }

    @Test
    fun `addReport calls repository insertReport`() = runTest {
        viewModel.addReport(sampleReport)
        advanceUntilIdle()
        coVerify { repository.insertReport(sampleReport) }
    }

    @Test
    fun `deleteReportById calls repository deleteReportById`() = runTest {
        viewModel.deleteReportById("rep1")
        advanceUntilIdle()
        coVerify { repository.deleteReportById("rep1") }
    }

    // ───── Calendar Notes ─────

    @Test
    fun `allNoteDates emits list from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf("2025-06-22"), viewModel.allNoteDates.value)
    }

    @Test
    fun `loadNoteForDate sets currentNote from repository`() = runTest {
        val note = CalendarNoteEntity("2025-06-22", "Drink water")
        coEvery { repository.getNoteByDate("2025-06-22") } returns note

        viewModel.loadNoteForDate("2025-06-22")
        advanceUntilIdle()

        assertEquals(note, viewModel.currentNote.value)
    }

    @Test
    fun `saveNote with non-blank text calls repository saveNote`() = runTest {
        viewModel.saveNote("2025-06-22", "Drink water")
        advanceUntilIdle()
        coVerify { repository.saveNote("2025-06-22", "Drink water") }
    }

    @Test
    fun `saveNote with blank text calls repository deleteNote`() = runTest {
        viewModel.saveNote("2025-06-22", "")
        advanceUntilIdle()
        coVerify { repository.deleteNote("2025-06-22") }
    }

    // ───── ViewModel Factory ─────

    @Test
    fun `HealthViewModelFactory creates HealthViewModel`() {
        val factory = HealthViewModelFactory(repository)
        val vm = factory.create(HealthViewModel::class.java)
        assert(vm is HealthViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `HealthViewModelFactory throws for unknown class`() {
        val factory = HealthViewModelFactory(repository)
        factory.create(androidx.lifecycle.AndroidViewModel::class.java)
    }
}
