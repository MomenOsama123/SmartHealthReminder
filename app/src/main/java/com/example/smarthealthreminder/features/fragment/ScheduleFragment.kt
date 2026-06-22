package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.adapter.CalendarDay
import com.example.smarthealthreminder.features.adapter.CalendarDayAdapter
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {

    private var recyclerSchedule: RecyclerView? = null
    private var recyclerCalendar: RecyclerView? = null
    private var scheduleAdapter: ScheduleAdapter? = null
    private var calendarDayAdapter: CalendarDayAdapter? = null
    private var selectedDate = ""
    private var currentCalendar = Calendar.getInstance()
    private var allItems = listOf<ScheduleItem>()
    private var eventDates = setOf<String>()
    private var noteDates = setOf<String>()
    private var reportDates = setOf<String>()
    private var scheduleEntryDates = setOf<String>()

    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDate = dateSdf.format(Date())

        // Calendar Grid
        recyclerCalendar = view.findViewById(R.id.recycler_calendar)
        recyclerCalendar?.layoutManager = GridLayoutManager(requireContext(), 7)
        calendarDayAdapter = CalendarDayAdapter { date ->
            selectedDate = date
            val cal = dateSdf.parse(date)
            val displayDate = cal?.let { displaySdf.format(it) } ?: date
            val intent = Intent(requireContext(), com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity::class.java).apply {
                putExtra(com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity.EXTRA_DATE, date)
                putExtra(com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity.EXTRA_DATE_DISPLAY, displayDate)
            }
            startActivity(intent)
        }
        recyclerCalendar?.adapter = calendarDayAdapter

        // Events List
        recyclerSchedule = view.findViewById(R.id.recycler_schedule)
        recyclerSchedule?.layoutManager = LinearLayoutManager(requireContext())
        scheduleAdapter = ScheduleAdapter()
        recyclerSchedule?.adapter = scheduleAdapter

        // Month navigation
        view.findViewById<ImageButton>(R.id.btn_prev_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            buildCalendar()
        }
        view.findViewById<ImageButton>(R.id.btn_next_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            buildCalendar()
        }





        // Collect all data sources
        collectAllData()

        buildCalendar()
        onDateSelected(selectedDate)
        viewModel.loadNoteForDate(selectedDate)
    }

    private fun collectAllData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { collectRemindersAndAlarms() }
                launch { collectScheduleEntries() }
                launch { collectReports() }
                launch { collectNotes() }
            }
        }
    }

    private suspend fun collectRemindersAndAlarms() {
        viewModel.allReminders.collect { reminders ->
            val reminderItems = reminders.map {
                ScheduleItem(
                    id = it.id,
                    title = it.title,
                    date = normalizeDate(it.date),
                    time = it.time ?: "No time",
                    category = it.category ?: "General",
                    priority = it.priority ?: "NORMAL",
                    status = it.status,
                    earlyNotification = it.earlyNotification,
                    earlyNotificationMinutes = it.earlyNotificationMinutes,
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_REMINDER
                )
            }
            val alarmItems = viewModel.allAlarms.value.filter { it.isActive }.map {
                ScheduleItem(
                    id = it.id,
                    title = it.label,
                    date = "daily",
                    time = "${it.time} ${it.amPm}",
                    category = it.category ?: "Alarm",
                    priority = "NORMAL",
                    status = "Pending",
                    isAlarm = true,
                    itemType = ScheduleItem.TYPE_ALARM
                )
            }
            allItems = (allItems.filter { it.itemType != ScheduleItem.TYPE_REMINDER && it.itemType != ScheduleItem.TYPE_ALARM } + reminderItems + alarmItems).sortedBy { it.time }
            eventDates = (eventDates + reminderItems.map { it.date }.filter { it.isNotBlank() && it != "daily" }).toSet()
            buildCalendar()
            applyFilter()
        }
    }

    private suspend fun collectScheduleEntries() {
        viewModel.allScheduleEntries.collect { entries ->
            val entryItems = entries.map {
                ScheduleItem(
                    id = it.id,
                    title = it.title,
                    date = it.date ?: "",
                    time = it.time ?: "No time",
                    category = it.category ?: "Schedule",
                    priority = "NORMAL",
                    status = "Pending",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_SCHEDULE_ENTRY
                )
            }
            allItems = (allItems.filter { it.itemType != ScheduleItem.TYPE_SCHEDULE_ENTRY } + entryItems).sortedBy { it.time }
            scheduleEntryDates = entryItems.map { it.date }.filter { it.isNotBlank() }.toSet()
            buildCalendar()
            applyFilter()
        }
    }

    private suspend fun collectReports() {
        viewModel.allReports.collect { reports ->
            val reportItems = reports.map {
                ScheduleItem(
                    id = it.id,
                    title = it.title,
                    date = it.date ?: "",
                    time = "Report",
                    category = "Report",
                    priority = "NORMAL",
                    status = "Completed",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_REPORT
                )
            }
            allItems = (allItems.filter { it.itemType != ScheduleItem.TYPE_REPORT } + reportItems).sortedBy { it.time }
            reportDates = reportItems.map { it.date }.filter { it.isNotBlank() }.toSet()
            buildCalendar()
            applyFilter()
        }
    }

    private suspend fun collectNotes() {
        viewModel.allNoteDates.collect { dates ->
            noteDates = dates.filter { it.isNotBlank() }.toSet()
            buildCalendar()
        }
    }

    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf1.isLenient = false
            val date1 = sdf1.parse(dateStr)
            if (date1 != null) return dateSdf.format(date1)

            val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            sdf2.isLenient = false
            val date2 = sdf2.parse(dateStr)
            if (date2 != null) return dateSdf.format(date2)

            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun buildCalendar() {
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = dateSdf.format(Date())
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        view?.findViewById<TextView>(R.id.tv_month_year)?.text = monthSdf.format(cal.time)

        val days = mutableListOf<CalendarDay>()

        repeat(firstDayOfWeek) {
            days.add(CalendarDay("", 0, false, false, false, false, false, false))
        }

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            days.add(CalendarDay(
                date = dateStr,
                dayNumber = day,
                isCurrentMonth = true,
                isToday = dateStr == today,
                hasEvents = eventDates.contains(dateStr),
                hasNotes = noteDates.contains(dateStr),
                hasReports = reportDates.contains(dateStr),
                hasScheduleEntries = scheduleEntryDates.contains(dateStr)
            ))
        }

        calendarDayAdapter?.setDays(days, selectedDate)
    }

    private fun onDateSelected(date: String) {
        val cal = dateSdf.parse(date)
        view?.findViewById<TextView>(R.id.tv_selected_date)?.text =
            if (date == dateSdf.format(Date())) "Today"
            else cal?.let { displaySdf.format(it) } ?: date



        viewModel.loadNoteForDate(date)
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = allItems.filter {
            it.date == selectedDate || it.date == "daily" || it.date.isBlank()
        }

        scheduleAdapter?.submitList(filtered)

        val pending = filtered.count { it.status.uppercase() == "PENDING" }
        val done = filtered.count { it.status.uppercase() in listOf("DONE", "COMPLETED") }

        view?.findViewById<TextView>(R.id.tv_total_count)?.text = filtered.size.toString()
        view?.findViewById<TextView>(R.id.tv_pending_count)?.text = pending.toString()
        view?.findViewById<TextView>(R.id.tv_done_count)?.text = done.toString()

        val isEmpty = filtered.isEmpty()
        view?.findViewById<View>(R.id.layout_empty)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
        recyclerSchedule?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerSchedule = null
        recyclerCalendar = null
        scheduleAdapter = null
        calendarDayAdapter = null
    }
}