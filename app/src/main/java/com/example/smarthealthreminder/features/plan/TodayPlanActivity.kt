package com.example.smarthealthreminder.features.plan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TodayPlanActivity : AppCompatActivity() {

    private lateinit var viewModel: HealthViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var emptyMessage: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCount: TextView

    private val todaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val today = todaySdf.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_plan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        viewModel = HealthViewModelFactory(repository).create(HealthViewModel::class.java)

        initViews()
        setupRecyclerView()
        observeData()
    }

    private fun initViews() {
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        tvDate = findViewById(R.id.tv_date)
        tvDate.text = displaySdf.format(Date())

        tvCount = findViewById(R.id.tv_count)
        emptyView = findViewById(R.id.layout_empty)
        emptyMessage = findViewById(R.id.tv_empty_message)
        recyclerView = findViewById(R.id.recycler_today_plan)

        emptyMessage.text = "You don't have any plans scheduled for today."

        findViewById<MaterialButton>(R.id.btn_add_reminder).setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btn_add_alarm).setOnClickListener {
            startActivity(Intent(this, EditAlarmActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btn_add_note).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SCHEDULE)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }

        findViewById<MaterialButton>(R.id.btn_create_report).setOnClickListener {
            startActivity(Intent(this, com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btn_open_schedule).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SCHEDULE)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
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
                viewModel.allReminders.collect { refreshList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allAlarms.collect { refreshList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allScheduleEntries.collect { refreshList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReports.collect { refreshList() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allNoteDates.collect { refreshList() }
            }
        }
    }

    private fun refreshList() {
        val reminderItems = viewModel.allReminders.value.filter {
            normalizeDate(it.date) == today
        }.map { reminder ->
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

        val scheduleEntryItems = viewModel.allScheduleEntries.value.filter {
            it.date == today
        }.map { entry ->
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

        val reportItems = viewModel.allReports.value.filter {
            it.date == today
        }.map { report ->
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

        val noteItems = viewModel.allNoteDates.value.filter { it == today }.map { noteDate ->
            ScheduleItem(
                id = noteDate,
                title = "Note for today",
                date = noteDate,
                time = "Note",
                category = "Note",
                priority = "NORMAL",
                status = "Completed",
                isAlarm = false,
                itemType = ScheduleItem.TYPE_NOTE
            )
        }

        val allItems = (reminderItems + alarmItems + scheduleEntryItems + reportItems + noteItems).sortedBy { it.time }
        scheduleAdapter.submitList(allItems)

        tvCount.text = "${allItems.size} items"

        val isEmpty = allItems.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
