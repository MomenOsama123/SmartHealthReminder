package com.example.smarthealthreminder.features.schedule.details

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.CalendarNoteEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DayDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_DATE_DISPLAY = "extra_date_display"
    }

    private lateinit var viewModel: HealthViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var recyclerView: RecyclerView

    // Empty state views
    private lateinit var layoutEmpty: View
    private lateinit var tvEmptyMessage: TextView
    private lateinit var btnAddNote: MaterialButton

    // Content header
    private lateinit var tvDate: TextView

    private var date: String = ""
    private var dateDisplay: String = ""
    private var currentNoteText: String = ""

    private val addNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Note was saved — reload so dot appears and list refreshes
            viewModel.loadNoteForDate(date)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        date = intent.getStringExtra(EXTRA_DATE) ?: run {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        dateDisplay = intent.getStringExtra(EXTRA_DATE_DISPLAY) ?: date

        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        viewModel = HealthViewModelFactory(repository).create(HealthViewModel::class.java)

        initViews()
        setupRecyclerView()
        observeData()
        viewModel.loadNoteForDate(date)
    }

    private fun initViews() {
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        tvDate = findViewById(R.id.tv_date)
        tvDate.text = dateDisplay

        recyclerView = findViewById(R.id.recycler_events)
        layoutEmpty = findViewById(R.id.layout_empty)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        btnAddNote = findViewById(R.id.btn_add_note)

        btnAddNote.setOnClickListener {
            addNoteLauncher.launch(
                Intent(this, AddNoteActivity::class.java).apply {
                    putExtra(AddNoteActivity.EXTRA_DATE, date)
                    putExtra(AddNoteActivity.EXTRA_DATE_DISPLAY, dateDisplay)
                    putExtra(AddNoteActivity.EXTRA_EXISTING_NOTE, currentNoteText)
                }
            )
        }

        // Quick action buttons (always visible at bottom)
        findViewById<MaterialButton>(R.id.btn_add_reminder).setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_add_alarm).setOnClickListener {
            startActivity(Intent(this, EditAlarmActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = scheduleAdapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReminders.collect { refreshEventList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allAlarms.collect { refreshEventList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allScheduleEntries.collect { refreshEventList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReports.collect { refreshEventList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentNote.collect { note ->
                    currentNoteText = note?.note ?: ""
                    refreshEventList()
                }
            }
        }
    }

    private fun refreshEventList() {
        val reminderItems = viewModel.allReminders.value
            .filter { normalizeDate(it.date) == date }
            .map { reminder ->
                ScheduleItem(
                    id = reminder.id,
                    title = reminder.title,
                    date = normalizeDate(reminder.date),
                    time = reminder.time ?: "No time",
                    category = reminder.category ?: "General",
                    priority = reminder.priority ?: "NORMAL",
                    status = reminder.status,
                    earlyNotification = reminder.earlyNotification,
                    earlyNotificationMinutes = reminder.earlyNotificationMinutes,
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_REMINDER
                )
            }

        val alarmItems = viewModel.allAlarms.value.filter { it.isActive }.map { alarm ->
            ScheduleItem(
                id = alarm.id,
                title = alarm.label,
                date = "daily",
                time = "${alarm.time} ${alarm.amPm}",
                category = alarm.category ?: "Alarm",
                priority = "NORMAL",
                status = alarm.lastTriggeredStatus,
                isAlarm = true,
                itemType = ScheduleItem.TYPE_ALARM
            )
        }

        val scheduleEntryItems = viewModel.allScheduleEntries.value
            .filter { it.date == date }
            .map { entry ->
                ScheduleItem(
                    id = entry.id,
                    title = entry.title,
                    date = entry.date ?: "",
                    time = entry.time ?: "No time",
                    category = entry.category ?: "Schedule",
                    priority = "NORMAL",
                    status = "Pending",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_SCHEDULE_ENTRY
                )
            }

        val reportItems = viewModel.allReports.value
            .filter { it.date == date }
            .map { report ->
                ScheduleItem(
                    id = report.id,
                    title = report.title,
                    date = report.date ?: "",
                    time = "Report",
                    category = "Report",
                    priority = "NORMAL",
                    status = "Completed",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_REPORT
                )
            }

        val noteItem = viewModel.currentNote.value
            ?.takeIf { it.note.isNotBlank() }
            ?.let { note ->
                ScheduleItem(
                    id = note.date,
                    title = note.note.take(60) + if (note.note.length > 60) "…" else "",
                    date = note.date,
                    time = "Note",
                    category = "Note",
                    priority = "NORMAL",
                    status = "Completed",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_NOTE
                )
            }

        val allItems = (reminderItems + alarmItems + scheduleEntryItems + reportItems +
                listOfNotNull(noteItem)).sortedBy { it.time }

        scheduleAdapter.submitList(allItems)

        val isEmpty = allItems.isEmpty()

        if (isEmpty) {
            // Empty state — show Add Note prompt
            layoutEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvEmptyMessage.text = "No events for this day.\nYou can add a note to remember something."
            btnAddNote.visibility = View.VISIBLE
        } else {
            // Has content — show list; note button still accessible via edit icon if note exists
            layoutEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            // Show "Edit / Add Note" button below the list always
            btnAddNote.visibility = View.VISIBLE
            btnAddNote.text = if (currentNoteText.isNotBlank()) "Edit Note" else "Add Note"
        }
    }

    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf1.isLenient = false
            val date1 = sdf1.parse(dateStr)
            if (date1 != null) return sdf1.format(date1)

            val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            sdf2.isLenient = false
            val date2 = sdf2.parse(dateStr)
            if (date2 != null) return sdf1.format(date2)

            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
