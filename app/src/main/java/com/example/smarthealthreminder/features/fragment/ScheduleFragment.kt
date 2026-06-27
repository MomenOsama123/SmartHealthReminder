package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.adapter.CalendarDay
import com.example.smarthealthreminder.features.adapter.CalendarDayAdapter
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {

    private enum class CalendarViewMode { MONTH, WEEK }

    private var recyclerCalendar: RecyclerView? = null
    private var recyclerScheduleItems: RecyclerView? = null
    private var calendarDayAdapter: CalendarDayAdapter? = null
    private var scheduleAdapter: ScheduleAdapter? = null

    private var layoutDowHeader: View? = null
    private var tvScheduleEmpty: TextView? = null
    private var toggleCalendarMode: MaterialButtonToggleGroup? = null

    private var selectedDate = ""
    private var currentCalendar = Calendar.getInstance()
    private var viewMode = CalendarViewMode.MONTH
    private var currentNoteText = ""

    private var eventDates = setOf<String>()
    private var noteDates = setOf<String>()
    private var reportDates = setOf<String>()
    private var scheduleEntryDates = setOf<String>()
    private var allItems = listOf<ScheduleItem>()

    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val weekRangeSdf = SimpleDateFormat("MMM d", Locale.getDefault())

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

        selectedDate = savedInstanceState?.getString(STATE_SELECTED_DATE)
            ?: dateSdf.format(Date())
        viewMode = savedInstanceState?.getString(STATE_VIEW_MODE)?.let {
            runCatching { CalendarViewMode.valueOf(it) }.getOrDefault(CalendarViewMode.MONTH)
        } ?: CalendarViewMode.MONTH

        layoutDowHeader = view.findViewById(R.id.layout_dow_header)
        tvScheduleEmpty = view.findViewById(R.id.tv_schedule_empty)
        toggleCalendarMode = view.findViewById(R.id.toggle_calendar_mode)

        recyclerCalendar = view.findViewById(R.id.recycler_calendar)
        recyclerScheduleItems = view.findViewById(R.id.recycler_schedule_items)

        calendarDayAdapter = CalendarDayAdapter { date ->
            selectedDate = date
            syncCalendarToSelectedDate()
            buildCalendar()
            updateSelectedDateLabel()
            refreshInlineList()
            viewModel.loadNoteForDate(selectedDate)
        }
        recyclerCalendar?.adapter = calendarDayAdapter

        scheduleAdapter = ScheduleAdapter { openDayDetails(selectedDate) }
        recyclerScheduleItems?.layoutManager = LinearLayoutManager(requireContext())
        recyclerScheduleItems?.adapter = scheduleAdapter

        setupNavigation(view)
        setupViewModeToggle()
        observeData()

        applyViewMode()
        buildCalendar()
        updateSelectedDateLabel()
        refreshInlineList()
        viewModel.loadNoteForDate(selectedDate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_DATE, selectedDate)
        outState.putString(STATE_VIEW_MODE, viewMode.name)
    }

    override fun onResume() {
        super.onResume()
        buildCalendar()
        updateSelectedDateLabel()
        refreshInlineList()
    }

    private fun setupNavigation(view: View) {
        view.findViewById<ImageButton>(R.id.btn_prev_month)?.setOnClickListener {
            if (viewMode == CalendarViewMode.MONTH) {
                currentCalendar.add(Calendar.MONTH, -1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            }
            buildCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month)?.setOnClickListener {
            if (viewMode == CalendarViewMode.MONTH) {
                currentCalendar.add(Calendar.MONTH, 1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            buildCalendar()
        }

        view.findViewById<MaterialButton>(R.id.btn_today)?.setOnClickListener {
            goToToday()
        }

        view.findViewById<MaterialButton>(R.id.btn_add)?.setOnClickListener {
            openDayDetails(selectedDate)
        }
    }

    private fun setupViewModeToggle() {
        toggleCalendarMode?.check(
            if (viewMode == CalendarViewMode.MONTH) R.id.btn_mode_month else R.id.btn_mode_week
        )

        toggleCalendarMode?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewMode = when (checkedId) {
                R.id.btn_mode_week -> CalendarViewMode.WEEK
                else -> CalendarViewMode.MONTH
            }
            syncCalendarToSelectedDate()
            applyViewMode()
            buildCalendar()
        }
    }

    private fun goToToday() {
        selectedDate = dateSdf.format(Date())
        currentCalendar = Calendar.getInstance()
        applyViewMode()
        buildCalendar()
        updateSelectedDateLabel()
        refreshInlineList()
        viewModel.loadNoteForDate(selectedDate)
    }

    private fun applyViewMode() {
        val isWeek = viewMode == CalendarViewMode.WEEK
        calendarDayAdapter?.isWeekMode = isWeek
        layoutDowHeader?.visibility = if (isWeek) View.GONE else View.VISIBLE

        recyclerCalendar?.layoutManager = if (isWeek) {
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        } else {
            GridLayoutManager(requireContext(), 7)
        }
    }

    private fun syncCalendarToSelectedDate() {
        val parsed = dateSdf.parse(selectedDate) ?: return
        currentCalendar = Calendar.getInstance().apply { time = parsed }
        if (viewMode == CalendarViewMode.WEEK) {
            currentCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        viewModel.allReminders,
                        viewModel.allAlarms,
                        viewModel.allScheduleEntries,
                        viewModel.allReports,
                        viewModel.allNoteDates
                    ) { reminders, alarms, entries, reports, notes ->
                        DataSnapshot(reminders, alarms, entries, reports, notes)
                    }.collect { snapshot ->
                        rebuildAllItems(snapshot)
                        buildCalendar()
                        updateSelectedDateLabel()
                        refreshInlineList()
                    }
                }
                launch {
                    viewModel.currentNote.collect { note ->
                        currentNoteText = note?.note.orEmpty()
                        refreshInlineList()
                    }
                }
            }
        }
    }

    private fun rebuildAllItems(snapshot: DataSnapshot) {
        val reminderItems = snapshot.reminders.map {
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

        val alarmItems = snapshot.alarms.filter { it.isActive }.map {
            ScheduleItem(
                id = it.id,
                title = it.label,
                date = "daily",
                time = "${it.time} ${it.amPm}",
                category = it.category ?: "Alarm",
                priority = "NORMAL",
                status = it.lastTriggeredStatus,
                isAlarm = true,
                itemType = ScheduleItem.TYPE_ALARM
            )
        }

        val entryItems = snapshot.entries.map {
            ScheduleItem(
                id = it.id,
                title = it.title,
                date = it.date.orEmpty(),
                time = it.time ?: "No time",
                category = it.category ?: "Schedule",
                priority = "NORMAL",
                status = "Pending",
                isAlarm = false,
                itemType = ScheduleItem.TYPE_SCHEDULE_ENTRY
            )
        }

        val reportItems = snapshot.reports.map {
            ScheduleItem(
                id = it.id,
                title = it.title,
                date = it.date.orEmpty(),
                time = "Report",
                category = "Report",
                priority = "NORMAL",
                status = "Completed",
                isAlarm = false,
                itemType = ScheduleItem.TYPE_REPORT
            )
        }

        allItems = (reminderItems + alarmItems + entryItems + reportItems).sortedBy { it.time }

        eventDates = reminderItems.map { it.date }
            .filter { it.isNotBlank() && it != "daily" }
            .toSet()
        scheduleEntryDates = entryItems.map { it.date }.filter { it.isNotBlank() }.toSet()
        reportDates = reportItems.map { it.date }.filter { it.isNotBlank() }.toSet()
        noteDates = snapshot.noteDates.filter { it.isNotBlank() }.toSet()
    }

    private fun buildCalendar() {
        if (viewMode == CalendarViewMode.WEEK) {
            buildWeekCalendar()
        } else {
            buildMonthCalendar()
        }
    }

    private fun buildMonthCalendar() {
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
            days.add(CalendarDay("", 0, false, false, false))
        }

        for (day in 1..daysInMonth) {
            days.add(createCalendarDay(year, month, day, today))
        }

        calendarDayAdapter?.setDays(days, selectedDate)
    }

    private fun buildWeekCalendar() {
        val weekStart = currentCalendar.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_YEAR, 6)

        val header = "${weekRangeSdf.format(weekStart.time)} – ${weekRangeSdf.format(weekEnd.time)}, ${weekStart.get(Calendar.YEAR)}"
        view?.findViewById<TextView>(R.id.tv_month_year)?.text = header

        val today = dateSdf.format(Date())
        val days = mutableListOf<CalendarDay>()

        val cursor = weekStart.clone() as Calendar
        repeat(7) {
            days.add(
                createCalendarDay(
                    cursor.get(Calendar.YEAR),
                    cursor.get(Calendar.MONTH),
                    cursor.get(Calendar.DAY_OF_MONTH),
                    today
                )
            )
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        calendarDayAdapter?.setDays(days, selectedDate)
    }

    private fun createCalendarDay(year: Int, month: Int, day: Int, today: String): CalendarDay {
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        return CalendarDay(
            date = dateStr,
            dayNumber = day,
            isCurrentMonth = month == currentCalendar.get(Calendar.MONTH),
            isToday = dateStr == today,
            hasEvents = eventDates.contains(dateStr),
            hasNotes = noteDates.contains(dateStr),
            hasReports = reportDates.contains(dateStr),
            hasScheduleEntries = scheduleEntryDates.contains(dateStr)
        )
    }

    private fun refreshInlineList() {
        val items = getItemsForDate(selectedDate)
        scheduleAdapter?.submitList(items)

        val isEmpty = items.isEmpty()
        tvScheduleEmpty?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerScheduleItems?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun getItemsForDate(date: String): List<ScheduleItem> {
        val dateItems = allItems.filter { item ->
            item.itemType == ScheduleItem.TYPE_ALARM ||
                    normalizeDate(item.date) == date ||
                    item.date == date
        }.toMutableList()

        if (noteDates.contains(date)) {
            val noteTitle = currentNoteText.takeIf { it.isNotBlank() }
                ?.let { text -> text.take(60) + if (text.length > 60) "…" else "" }
                ?: "Note for this day"
            dateItems.add(
                ScheduleItem(
                    id = date,
                    title = noteTitle,
                    date = date,
                    time = "Note",
                    category = "Note",
                    priority = "NORMAL",
                    status = "Completed",
                    isAlarm = false,
                    itemType = ScheduleItem.TYPE_NOTE
                )
            )
        }

        return dateItems.sortedBy { it.time }
    }

    private fun updateSelectedDateLabel() {
        val today = dateSdf.format(Date())
        val cal = dateSdf.parse(selectedDate)
        val label = if (selectedDate == today) {
            getString(R.string.today)
        } else {
            cal?.let { displaySdf.format(it) } ?: selectedDate
        }

        view?.findViewById<TextView>(R.id.tv_selected_date)?.text = label

        val count = getItemsForDate(selectedDate).size
        view?.findViewById<TextView>(R.id.tv_events_count)?.text =
            if (count > 0) "$count item${if (count > 1) "s" else ""}" else ""
    }

    private fun openDayDetails(date: String) {
        val cal = dateSdf.parse(date)
        val displayDate = if (date == dateSdf.format(Date())) {
            getString(R.string.today)
        } else {
            cal?.let { displaySdf.format(it) } ?: date
        }
        startActivity(
            Intent(requireContext(), DayDetailsActivity::class.java).apply {
                putExtra(DayDetailsActivity.EXTRA_DATE, date)
                putExtra(DayDetailsActivity.EXTRA_DATE_DISPLAY, displayDate)
            }
        )
    }

    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }
            val date1 = sdf1.parse(dateStr)
            if (date1 != null) return dateSdf.format(date1)

            val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply { isLenient = false }
            val date2 = sdf2.parse(dateStr)
            if (date2 != null) return dateSdf.format(date2)

            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCalendar = null
        recyclerScheduleItems = null
        calendarDayAdapter = null
        scheduleAdapter = null
        layoutDowHeader = null
        tvScheduleEmpty = null
        toggleCalendarMode = null
    }

    private data class DataSnapshot(
        val reminders: List<com.example.smarthealthreminder.features.data.local.entity.ReminderEntity>,
        val alarms: List<com.example.smarthealthreminder.features.data.local.entity.AlarmEntity>,
        val entries: List<com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity>,
        val reports: List<com.example.smarthealthreminder.features.data.local.entity.ReportEntity>,
        val noteDates: List<String>
    )

    companion object {
        private const val STATE_SELECTED_DATE = "state_selected_date"
        private const val STATE_VIEW_MODE = "state_view_mode"
    }
}
