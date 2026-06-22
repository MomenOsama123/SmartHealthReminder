package com.example.smarthealthreminder.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smarthealthreminder.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.data.local.entity.ReportEntity
import com.example.smarthealthreminder.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.data.repository.HealthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HealthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: HealthRepository
    private lateinit var viewModel: HealthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = HealthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `allAlarms emits empty list by default`() = runTest {
        coEvery { repository.getAllAlarms() } returns flowOf(emptyList())
        val factory = HealthViewModelFactory(repository)
        val vm = factory.create(HealthViewModel::class.java)
        assertEquals(emptyList<AlarmEntity>(), vm.allAlarms.value)
    }

    @Test
    fun `addReminder calls repository insertReminder`() = runTest {
        val reminder = ReminderEntity(id = "1", title = "Test", status = "Pending")
        coEvery { repository.insertReminder(reminder) } returns Unit
        viewModel.addReminder(reminder)
        coVerify(exactly = 1) { repository.insertReminder(reminder) }
    }

    @Test
    fun `deleteReminderById calls repository deleteReminderById`() = runTest {
        coEvery { repository.deleteReminderById("1") } returns Unit
        viewModel.deleteReminderById("1")
        coVerify(exactly = 1) { repository.deleteReminderById("1") }
    }

    @Test
    fun `markReminderDone calls repository markReminderDone`() = runTest {
        coEvery { repository.markReminderDone("1") } returns Unit
        viewModel.markReminderDone("1")
        coVerify(exactly = 1) { repository.markReminderDone("1") }
    }

    @Test
    fun `addAlarm calls repository insertAlarm`() = runTest {
        val alarm = AlarmEntity(id = "1", label = "Alarm", time = "08:00", amPm = "AM", category = "MEDICINE")
        coEvery { repository.insertAlarm(alarm) } returns Unit
        viewModel.addAlarm(alarm)
        coVerify(exactly = 1) { repository.insertAlarm(alarm) }
    }

    @Test
    fun `toggleAlarm calls repository toggleAlarmStatus`() = runTest {
        coEvery { repository.toggleAlarmStatus("1", true) } returns Unit
        viewModel.toggleAlarm("1", true)
        coVerify(exactly = 1) { repository.toggleAlarmStatus("1", true) }
    }

    @Test
    fun `saveNote calls repository saveNote`() = runTest {
        coEvery { repository.saveNote("2024-01-01", "Note text") } returns Unit
        viewModel.saveNote("2024-01-01", "Note text")
        coVerify(exactly = 1) { repository.saveNote("2024-01-01", "Note text") }
    }

    @Test
    fun `addScheduleEntry calls repository insertScheduleEntry`() = runTest {
        val entry = ScheduleEntryEntity(id = "1", title = "Entry", date = "2024-01-01")
        coEvery { repository.insertScheduleEntry(entry) } returns Unit
        viewModel.addScheduleEntry(entry)
        coVerify(exactly = 1) { repository.insertScheduleEntry(entry) }
    }

    @Test
    fun `addReport calls repository insertReport`() = runTest {
        val report = ReportEntity(id = "1", title = "Report", date = "2024-01-01")
        coEvery { repository.insertReport(report) } returns Unit
        viewModel.addReport(report)
        coVerify(exactly = 1) { repository.insertReport(report) }
    }

    @Test
    fun `loadNoteForDate updates currentNote`() = runTest {
        val note = CalendarNoteEntity("2024-01-01", "Test note")
        coEvery { repository.getNoteByDate("2024-01-01") } returns note
        viewModel.loadNoteForDate("2024-01-01")
        assertEquals(note, viewModel.currentNote.value)
    }

    @Test
    fun `viewModelFactory creates correct instance`() {
        val factory = HealthViewModelFactory(repository)
        val vm = factory.create(HealthViewModel::class.java)
        assertNotNull(vm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `viewModelFactory throws for unknown class`() {
        val factory = HealthViewModelFactory(repository)
        factory.create(UnsupportedOperationException::class.java)
    }
}
